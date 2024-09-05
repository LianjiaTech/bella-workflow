package com.ke.bella.workflow.trigger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.googlecode.aviator.AviatorEvaluator;
import com.ke.bella.workflow.TaskExecutor;
import com.ke.bella.workflow.db.repo.DataSourceRepo;
import com.ke.bella.workflow.db.repo.WorkflowTriggerRepo;
import com.ke.bella.workflow.db.tables.pojos.KafkaDatasourceDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowKafkaTriggerDB;
import com.ke.bella.workflow.utils.JsonUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class KafkaEventConsumers {

    @Autowired
    DataSourceRepo ds;

    @Autowired
    WorkflowTriggerRepo triggers;

    @Autowired
    WorkflowSchedulingTriggerHelper helper;

    final Map<String, ConcurrentMessageListenerContainer<String, String>> consumers = new ConcurrentHashMap<>();
    final Map<String, Set<String>> consumerDataSourceIdsMapping = new ConcurrentHashMap<>();
    final Map<String, KafkaDatasourceDB> activeDataSources = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // init consumers
        TaskExecutor.schedule(this::refresh, 1000);

        // try update consumers every 300s.
        TaskExecutor.scheduleAtFixedRate(this::refresh, 300);
    }

    void refresh() {
        List<KafkaDatasourceDB> dss = ds.listAllKafkaDs();

        for (KafkaDatasourceDB ds : dss) {
            String key = serverKey(ds.getServer(), ds.getTopic());
            if(consumerDataSourceIdsMapping.containsKey(key)) {
                if(ds.getStatus().intValue() == -1) {
                    consumerDataSourceIdsMapping.get(key).remove(ds.getDatasourceId());
                } else {
                    consumerDataSourceIdsMapping.get(key).add(ds.getDatasourceId());
                }
            } else {
                if(ds.getStatus().intValue() == 0) {
                    try {
                        addConsumer(ds.getServer(), ds.getTopic());

                        Set<String> dsIdSet = new HashSet<>();
                        dsIdSet.add(ds.getDatasourceId());
                        consumerDataSourceIdsMapping.put(key, dsIdSet);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to start kafka comsumer, server:{} topic:{}", ds.getServer(), ds.getTopic(), e);
                    }
                }
            }

            if(ds.getStatus().intValue() == 0) {
                activeDataSources.put(ds.getDatasourceId(), ds);
            }
        }

        List<String> removeKeys = new ArrayList<>();
        consumerDataSourceIdsMapping.forEach((k, s) -> {
            if(s.isEmpty()) {
                removeConsumer(k);
                removeKeys.add(k);
            }
        });

        for (String key : removeKeys) {
            consumerDataSourceIdsMapping.remove(key);
        }
    }

    void addConsumer(String server, String topic) {
        String key = serverKey(server, topic);
        ConsumerFactory<String, String> consumerFactory = createConsumerFactory(server, "bella-workflow");

        ContainerProperties containerProps = new ContainerProperties(topic);
        containerProps.setMessageListener(new EventListener(key));

        ConcurrentMessageListenerContainer<String, String> container = new ConcurrentMessageListenerContainer<>(consumerFactory, containerProps);

        container.start();
        consumers.put(key, container);
        LOGGER.info("Started consumer with server:{} topic:{} ", server, topic);
    }

    void removeConsumer(String id) {
        ConcurrentMessageListenerContainer<String, String> container = consumers.remove(id);
        if(container != null) {
            container.stop();
            LOGGER.info("Stopped and removed consumer with server:{} ", id);
        }
    }

    ConsumerFactory<String, String> createConsumerFactory(String bootstrapServers, String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    String serverKey(String server, String topic) {
        return server + "/" + topic;
    }

    void tryTrigger(Set<String> dataSourceIds, Object value) {
        List<WorkflowKafkaTriggerDB> ts = triggers.listAllActiveKafkaTriggers(dataSourceIds);
        List<WorkflowKafkaTriggerDB> ts2 = ts.stream().filter(t -> canTrigger(t, value))
                .collect(Collectors.toList());
        helper.tryKafkaTrigger(ts2, value);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    boolean canTrigger(WorkflowKafkaTriggerDB db, Object value) {
        Map env = new HashMap();
        env.put("event", value);

        Object res = AviatorEvaluator.execute(db.getId().toString(), db.getExpression(), env, true);
        return res instanceof Boolean && (Boolean) res;
    }

    public static void validate(String expression) {
        AviatorEvaluator.compile(expression);
    }

    class EventListener implements MessageListener<String, String> {
        final String key;

        public EventListener(String key) {
            this.key = key;
        }

        @Override
        public void onMessage(ConsumerRecord<String, String> data) {
            String value = data.value();
            if(!StringUtils.hasText(value)) {
                return;
            }

            KafkaEventConsumers.LOGGER.trace("recv kafka msg from {}, event: {}", key, value);

            Set<String> dsids = consumerDataSourceIdsMapping.get(key);
            if(dsids != null && !dsids.isEmpty()) {
                Object obj = JsonUtils.fromJson(value, Object.class);
                tryTrigger(dsids, obj);
            }
        }
    }
}
