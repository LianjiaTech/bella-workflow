package com.ke.bella.workflow.tasks.config;

import lombok.Data;

import java.util.List;

/**
 * Elasticsearch配置实体类
 */
@Data
public class ElasticsearchConfig {
    private List<String> hosts;
    private String indexTemplate;
    private String username;
    private String password;
    
    // 连接配置
    private int maxConnTotal;
    private int maxConnPerRoute;
    private int connectTimeout;
    private int socketTimeout;
    private int keepAliveTime;
    
    // 批量操作配置
    private int bulkFlushMaxActions;
    private int bulkFlushInterval;
    private boolean bulkFlushBackoffEnabled;
    private String bulkFlushBackoffType;
    private int bulkFlushBackoffDelay;
    private int bulkFlushBackoffRetries;
}
