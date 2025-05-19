package com.ke.bella.workflow.tasks.config;

import lombok.Data;

/**
 * Flink配置实体类
 */
@Data
public class FlinkConfig {
    // 检查点配置
    private long checkpointInterval;
    private long checkpointMinPause;
    private long checkpointTimeout;
    
    // 重启策略配置
    private int restartAttempts;
    private int restartDelaySeconds;
}
