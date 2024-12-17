package com.ke.bella.workflow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.util.CollectionUtils;

import com.google.common.collect.ImmutableMap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.XAddParams;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.resps.StreamEntry;

public class RedisMesh {

    public interface MessageListener {
        default void onMessage(Event e) {
            // no-op
        }

        default void onPrivateMessage(Event e) {
            onMessage(e);
        }

        default void onBroadcastMessage(Event e) {
            onMessage(e);
        }
    }

    @FunctionalInterface
    interface EventCallback {
        void onEvent(Event event);
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Event {
        String name;
        String from;
        String payload;
        String context;
    }

    @Getter
    private final String profile;
    @Getter
    private final String instanceId;
    @Getter
    private final String broadcastTopic;
    @Getter
    private final String instanceStreamKey;
    @Getter
    private final String broadcastStreamKey;
    private MessageListener defaultListener;
    private final JedisPool jedisPool;
    private ExecutorService executor;
    private final AtomicBoolean running;
    private final long keepFromSecs;
    private final Map<String, MessageListener> listeners;

    private static final String TASK_MAPPING_KEY = "redis-mesh:task-instance-hash";
    private static final String BROADCAST_STREAM_PREFIX = "redis-mesh:broadcast-stream:";
    private static final String PRIVATE_STREAM_PREFIX = "redis-mesh:private-stream:";
    private static final MessageListener DoNothingListener = new MessageListener() {
    };

    public RedisMesh(String profile, String instanceId, String broadcastTopic, JedisPool pool) {
        this(profile, instanceId, broadcastTopic, 60L, DoNothingListener, pool);
    }

    /**
     * @param instanceId     实例id
     * @param broadcastTopic 广播的topic名，建议是项目名
     * @param keepFromSecs   保留多长时间的历史消息
     * @param listener       消息监听器
     * @param pool
     */
    public RedisMesh(String profile, String instanceId, String broadcastTopic, long keepFromSecs, MessageListener listener, JedisPool pool) {
        this.profile = profile;
        this.instanceId = instanceId;
        this.broadcastTopic = broadcastTopic;
        this.instanceStreamKey = String.format("%s%s:%s", PRIVATE_STREAM_PREFIX, profile, instanceId);
        this.broadcastStreamKey = String.format("%s%s:%s", BROADCAST_STREAM_PREFIX, profile, broadcastTopic);
        this.keepFromSecs = keepFromSecs;
        this.defaultListener = listener;
        this.jedisPool = pool;
        this.running = new AtomicBoolean(false);
        this.listeners = new ConcurrentHashMap<>();
    }

    public void start() {
        if(this.running.get()) {
            return;
        }
        this.running.set(true);
        this.executor = Executors.newFixedThreadPool(2, new TaskExecutor.NamedThreadFactory("redis-mesh-", true));

        // 启动消息监听线程
        startMessageConsumers();
    }

    public void registerListener(String name, MessageListener listener) {
        this.listeners.putIfAbsent(name, listener);
    }

    public void sendPrivateMessage(String targetInstanceId, Event event) {
        String streamKey = String.format("%s%s:%s", PRIVATE_STREAM_PREFIX, profile, targetInstanceId);
        sendMessage(streamKey, event);
    }

    public void sendBroadcastMessage(Event event) {
        sendMessage(broadcastStreamKey, event);
    }

    public boolean addTask(String taskId, int expireSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(TASK_MAPPING_KEY, taskId, instanceId);
            if(expireSeconds > 0) {
                jedis.hexpire(TASK_MAPPING_KEY, expireSeconds, taskId);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void removeTask(String taskId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hdel(TASK_MAPPING_KEY, taskId);
        } catch (Exception e) {
            // no-op
        }
    }

    public String getInstanceId(String taskId) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hget(TASK_MAPPING_KEY, taskId);
        } catch (Exception e) {
            return "";
        }
    }

    public void shutdown() {
        running.set(false);
        executor.shutdown();
        jedisPool.close();
    }

    private void startMessageConsumers() {
        // 广播消息消费者
        executor.submit(this::consumeBroadcastMessages);

        // 私有消息消费者
        executor.submit(this::consumePrivateMessages);
    }

    private MessageListener getListener(Event e) {
        MessageListener listener = this.listeners.get(e.name);
        return listener == null ? defaultListener : listener;
    }

    private void consumePrivateMessages() {
        consumeMessages(instanceStreamKey, e -> getListener(e).onPrivateMessage(e));

    }

    private void consumeBroadcastMessages() {
        consumeMessages(broadcastStreamKey, e -> getListener(e).onBroadcastMessage(e));
    }

    private void consumeMessages(String streamKey, EventCallback callback) {
        String lastId = String.format("%s-0", System.currentTimeMillis());
        while (running.get()) {
            try (Jedis jedis = jedisPool.getResource()) {
                List<Entry<String, List<StreamEntry>>> sentries = jedis.xread(XReadParams.xReadParams().count(1).block(1000),
                        ImmutableMap.of(streamKey, new StreamEntryID(lastId)));
                if(CollectionUtils.isEmpty(sentries)) {
                    continue;
                }
                List<StreamEntry> entries = sentries.get(0).getValue();
                if(entries != null && !entries.isEmpty()) {
                    StreamEntry entry = entries.get(0);
                    Event event = convertMapToEvent(entry.getFields());
                    callback.onEvent(event);
                    lastId = entry.getID().toString();
                }
            } catch (Exception e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void sendMessage(String streamKey, Event event) {
        Map<String, String> message = convertEventToMap(event);

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.xadd(streamKey, XAddParams.xAddParams().minId(String.valueOf(System.currentTimeMillis() - keepFromSecs * 1000)), message);
        } catch (JedisException e) {
            throw new RuntimeException("Failed to send message to topic: " + streamKey, e);
        }
    }

    private Map<String, String> convertEventToMap(Event event) {
        Map<String, String> map = new HashMap<>();
        map.put("name", event.name);
        map.put("from", instanceId);
        map.put("payload", event.payload);
        map.put("context", event.context);
        return map;
    }

    private Event convertMapToEvent(Map<String, String> map) {
        Event event = new Event();
        event.name = map.get("name");
        event.from = map.get("from");
        event.payload = map.get("payload");
        event.context = map.get("context");
        return event;
    }
}
