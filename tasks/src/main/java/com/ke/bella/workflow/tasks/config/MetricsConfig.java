package com.ke.bella.workflow.tasks.config;

import lombok.Data;

/**
 * 监控指标配置实体类
 */
@Data
public class MetricsConfig {
    private String totalEvents;
    private String errorEvents;
    private String esWriteFailures;
}
