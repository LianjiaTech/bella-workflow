package com.ke.bella.workflow.tasks.config;

import lombok.Data;

import java.util.Properties;

/**
 * Kafka配置实体类
 */
@Data
public class KafkaConfig {
    private String bootstrapServers;
    private String groupId;
    private String topic;
    
    // 高级配置
    private String enableAutoCommit;
    private String autoOffsetReset;
    private String isolationLevel;
    private String maxPollRecords;
    private String sessionTimeoutMs;
    private String heartbeatIntervalMs;
    private String fetchMaxWaitMs;

    /**
     * 获取Kafka配置属性
     *
     * @return Kafka配置属性
     */
    public Properties toProperties() {
        Properties kafkaProps = new Properties();
        
        // 基本配置
        kafkaProps.setProperty("bootstrap.servers", bootstrapServers);
        kafkaProps.setProperty("group.id", groupId);
        
        // 高级配置
        kafkaProps.setProperty("enable.auto.commit", enableAutoCommit);
        kafkaProps.setProperty("auto.offset.reset", autoOffsetReset);
        kafkaProps.setProperty("isolation.level", isolationLevel);
        kafkaProps.setProperty("max.poll.records", maxPollRecords);
        kafkaProps.setProperty("session.timeout.ms", sessionTimeoutMs);
        kafkaProps.setProperty("heartbeat.interval.ms", heartbeatIntervalMs);
        kafkaProps.setProperty("fetch.max.wait.ms", fetchMaxWaitMs);

        return kafkaProps;
    }
}
