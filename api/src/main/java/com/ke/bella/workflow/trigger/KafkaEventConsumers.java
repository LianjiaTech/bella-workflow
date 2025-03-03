package com.ke.bella.workflow.trigger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ke.bella.workflow.TaskExecutor;
import com.ke.bella.workflow.db.repo.DataSourceRepo;
import com.ke.bella.workflow.db.repo.WorkflowTriggerRepo;
import com.ke.bella.workflow.db.tables.pojos.KafkaDatasourceDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowKafkaTriggerDB;
import com.ke.bella.workflow.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class KafkaEventConsumers {

    @Value("${spring.profiles.active}")
    String profile;

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
        List<KafkaDatasourceDB> dss = ds.listAllConsumerKafkaDs();

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
                        addConsumer(ds.getServer(), ds.getTopic(), ds.getAutoOffsetReset(), ds.getPropsConfig());

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

    void addConsumer(String server, String topic, String autoOffsetReset, String propsConfig) {
        String key = serverKey(server, topic);
        ConsumerFactory<String, String> consumerFactory = createConsumerFactory(server,
                "bella-workflow-" + profile, autoOffsetReset, propsConfig);

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

    ConsumerFactory<String, String> createConsumerFactory(String bootstrapServers, String groupId,
            String autoOffsetReset, String propsConfig) {
        Map<String, Object> props = new HashMap<>();
        if(StringUtils.hasText(propsConfig)) {
            Map<String, Object> clientConfigMap = JsonUtils.fromJson(propsConfig,
                    new TypeReference<Map<String, Object>>() {
                    });
            if(clientConfigMap != null) {
                props.putAll(clientConfigMap);
            }
        }

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        String autoOffset = StringUtils.isEmpty(autoOffsetReset) ? "latest" : autoOffsetReset;
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffset);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    String serverKey(String server, String topic) {
        return server + "/" + topic;
    }

    void tryTrigger(Set<String> dataSourceIds, String event) {
        List<WorkflowKafkaTriggerDB> ts = triggers.listAllActiveKafkaTriggers(dataSourceIds);

        if(!CollectionUtils.isEmpty(ts)) {
            KafkaEventConsumers.LOGGER.info("try trigger kafka msg from {}, event: {}", dataSourceIds.iterator().next(), event);

            Object value = JsonUtils.fromJson(event, Object.class);
            List<WorkflowKafkaTriggerDB> ts2 = ts.stream().filter(t -> canTrigger(t, value))
                    .collect(Collectors.toList());
            helper.tryKafkaTrigger(ts2, value);
        }
    }

    boolean canTrigger(WorkflowKafkaTriggerDB db, Object value) {
        return ExpressionHelper.canTrigger(db.getExpressionType(), db.getId().toString(), db.getExpression(), value);
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
                tryTrigger(dsids, value);
            }
        }
    }
}
