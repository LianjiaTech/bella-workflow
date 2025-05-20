package com.ke.bella.workflow.tasks.config;

import org.apache.flink.shaded.guava31.com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

public class AppConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);
    private static final String DEFAULT_CONFIG_FILE = "application.properties";
    private static AppConfig INSTANCE;
    private static final Properties PROPERTIES = new Properties();

    private static void initializeConfig(Properties properties) {
        String configFilePath = Optional.ofNullable(System.getenv("CONFIG_FILE"))
                .orElse(Optional.ofNullable((String) properties.get("config")).orElse(DEFAULT_CONFIG_FILE));

        File configFile = new File(configFilePath);

        LOGGER.info("[AppConfig] Attempting to load configuration file: {}", configFile.getAbsolutePath());

        InputStream input = null;
        try {
            if (configFile.exists() && configFile.isFile()) {
                LOGGER.info("[AppConfig] Loading configuration from filesystem path: {}", configFile.getAbsolutePath());
                input = Files.newInputStream(configFile.toPath());
            } else {
                LOGGER.info("[AppConfig] File not found, attempting to load default configuration from classpath: {}", DEFAULT_CONFIG_FILE);
                input = AppConfig.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE);
            }

            if (input == null) {
                LOGGER.error("[AppConfig] Unable to find configuration file: {}", configFilePath);
                throw new IllegalStateException("Unable to find configuration file: " + configFilePath);
            }

            PROPERTIES.load(input);
            LOGGER.info("[AppConfig] Successfully loaded configuration file: {}",
                    configFile.exists() ? configFile.getAbsolutePath() : DEFAULT_CONFIG_FILE);

            loadEnvironmentVariables();
        } catch (IOException ex) {
            LOGGER.error("[AppConfig] Error loading configuration file: {}", configFilePath, ex);
            throw new IllegalStateException(String.format("Error loading configuration file %s, exception: %s",
                    configFilePath, Throwables.getStackTraceAsString(ex)));
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    LOGGER.warn("[AppConfig] Error closing configuration file input stream", e);
                }
            }
        }
    }

    /**
     * 从环境变量加载配置，覆盖默认配置
     */
    private static void loadEnvironmentVariables() {
        // Kafka配置
        overrideFromEnv("KAFKA_BOOTSTRAP_SERVERS", "kafka.bootstrap.servers");
        overrideFromEnv("KAFKA_GROUP_ID", "kafka.group.id");
        overrideFromEnv("KAFKA_TOPIC", "kafka.topic");
        overrideFromEnv("KAFKA_ENABLE_AUTO_COMMIT", "kafka.enable.auto.commit");
        overrideFromEnv("KAFKA_AUTO_OFFSET_RESET", "kafka.auto.offset.reset");
        overrideFromEnv("KAFKA_ISOLATION_LEVEL", "kafka.isolation.level");
        overrideFromEnv("KAFKA_MAX_POLL_RECORDS", "kafka.max.poll.records");
        overrideFromEnv("KAFKA_SESSION_TIMEOUT_MS", "kafka.session.timeout.ms");
        overrideFromEnv("KAFKA_HEARTBEAT_INTERVAL_MS", "kafka.heartbeat.interval.ms");
        overrideFromEnv("KAFKA_FETCH_MAX_WAIT_MS", "kafka.fetch.max.wait.ms");

        // Elasticsearch配置
        overrideFromEnv("ELASTICSEARCH_HOSTS", "elasticsearch.hosts");
        overrideFromEnv("ELASTICSEARCH_INDEX_TEMPLATE", "elasticsearch.index.template");
        overrideFromEnv("ELASTICSEARCH_BULK_FLUSH_MAX_ACTIONS", "elasticsearch.bulk.flush.max.actions");
        overrideFromEnv("ELASTICSEARCH_BULK_FLUSH_INTERVAL", "elasticsearch.bulk.flush.interval");
        overrideFromEnv("ELASTICSEARCH_MAX_CONN_TOTAL", "elasticsearch.max.conn.total");
        overrideFromEnv("ELASTICSEARCH_MAX_CONN_PER_ROUTE", "elasticsearch.max.conn.per.route");
        overrideFromEnv("ELASTICSEARCH_USERNAME", "elasticsearch.username");
        overrideFromEnv("ELASTICSEARCH_PASSWORD", "elasticsearch.password");
        overrideFromEnv("ELASTICSEARCH_CONNECT_TIMEOUT", "elasticsearch.connect.timeout");
        overrideFromEnv("ELASTICSEARCH_SOCKET_TIMEOUT", "elasticsearch.socket.timeout");
        overrideFromEnv("ELASTICSEARCH_KEEP_ALIVE_TIME", "elasticsearch.keep.alive.time");

        // Elasticsearch Backoff策略
        overrideFromEnv("ELASTICSEARCH_BULK_FLUSH_BACKOFF_ENABLED", "elasticsearch.bulk.flush.backoff.enabled");
        overrideFromEnv("ELASTICSEARCH_BULK_FLUSH_BACKOFF_TYPE", "elasticsearch.bulk.flush.backoff.type");
        overrideFromEnv("ELASTICSEARCH_BULK_FLUSH_BACKOFF_DELAY", "elasticsearch.bulk.flush.backoff.delay");
        overrideFromEnv("ELASTICSEARCH_BULK_FLUSH_BACKOFF_RETRIES", "elasticsearch.bulk.flush.backoff.retries");

        // Flink配置
        overrideFromEnv("FLINK_CHECKPOINT_INTERVAL", "flink.checkpoint.interval");
        overrideFromEnv("FLINK_CHECKPOINT_TIMEOUT", "flink.checkpoint.timeout");
        overrideFromEnv("FLINK_CHECKPOINT_MIN_PAUSE", "flink.checkpoint.min.pause");
        overrideFromEnv("FLINK_RESTART_ATTEMPTS", "flink.restart.attempts");
        overrideFromEnv("FLINK_RESTART_DELAY_SECONDS", "flink.restart.delay.seconds");

        // 任务基本配置
        overrideFromEnv("JOB_NAME", "job.name");
        overrideFromEnv("ERROR_STREAM_NAME", "error.stream.name");

        // 指标配置
        overrideFromEnv("METRIC_TOTAL_EVENTS", "metric.total.events");
        overrideFromEnv("METRIC_ERROR_EVENTS", "metric.error.events");
        overrideFromEnv("METRIC_ES_WRITE_FAILURES", "metric.es.write.failures");

        // 验证模式配置
        overrideFromEnv("VALIDATION_JSON_PATTERN", "validation.json.pattern");
        overrideFromEnv("VALIDATION_RUN_LOG_PATTERN", "validation.run-log.pattern");
    }

    /**
     * 从环境变量覆盖配置
     *
     * @param envVar  环境变量名
     * @param propKey 属性键名
     */
    private static void overrideFromEnv(String envVar, String propKey) {
        String value = System.getenv(envVar);
        if (value != null && !value.isEmpty()) {
            LOGGER.info("[AppConfig] Overriding {} with value from environment variable {}", propKey, envVar);
            PROPERTIES.setProperty(propKey, value);
        }
    }

    public static synchronized AppConfig getInstance(Properties properties) {
        if (INSTANCE == null) {
            INSTANCE = new AppConfig();
            initializeConfig(properties);
        }
        return INSTANCE;
    }

    // ========================= 任务基本配置 =========================

    /**
     * 获取任务名称
     *
     * @return 任务名称
     */
    public String getJobName() {
        return PROPERTIES.getProperty("job.name");
    }

    /**
     * 获取错误流名称
     *
     * @return 错误流名称
     */
    public String getErrorStreamName() {
        return PROPERTIES.getProperty("error.stream.name");
    }

    // ========================= Kafka配置 =========================

    /**
     * 获取Kafka配置
     *
     * @return Kafka配置对象
     */
    public KafkaConfig getKafkaConfig() {
        KafkaConfig config = new KafkaConfig();

        // 基本配置
        config.setBootstrapServers(PROPERTIES.getProperty("kafka.bootstrap.servers"));
        config.setGroupId(PROPERTIES.getProperty("kafka.group.id"));
        config.setTopic(PROPERTIES.getProperty("kafka.topic"));

        // 高级配置
        config.setEnableAutoCommit(PROPERTIES.getProperty("kafka.enable.auto.commit"));
        config.setAutoOffsetReset(PROPERTIES.getProperty("kafka.auto.offset.reset"));
        config.setIsolationLevel(PROPERTIES.getProperty("kafka.isolation.level"));
        config.setMaxPollRecords(PROPERTIES.getProperty("kafka.max.poll.records"));
        config.setSessionTimeoutMs(PROPERTIES.getProperty("kafka.session.timeout.ms"));
        config.setHeartbeatIntervalMs(PROPERTIES.getProperty("kafka.heartbeat.interval.ms"));
        config.setFetchMaxWaitMs(PROPERTIES.getProperty("kafka.fetch.max.wait.ms"));

        return config;
    }

    // ========================= Elasticsearch配置 =========================

    /**
     * 获取Elasticsearch配置
     *
     * @return Elasticsearch配置对象
     */
    public ElasticsearchConfig getElasticsearchConfig() {
        ElasticsearchConfig config = new ElasticsearchConfig();

        // 基本配置
        String[] hosts = PROPERTIES.getProperty("elasticsearch.hosts").split(",");
        config.setHosts(Arrays.asList(hosts));
        config.setIndexTemplate(PROPERTIES.getProperty("elasticsearch.index.template"));

        // 认证配置
        config.setUsername(PROPERTIES.getProperty("elasticsearch.username"));
        config.setPassword(PROPERTIES.getProperty("elasticsearch.password"));

        // 连接配置
        String maxConnTotal = PROPERTIES.getProperty("elasticsearch.max.conn.total");
        if (maxConnTotal != null) {
            config.setMaxConnTotal(Integer.parseInt(maxConnTotal));
        }

        String maxConnPerRoute = PROPERTIES.getProperty("elasticsearch.max.conn.per.route");
        if (maxConnPerRoute != null) {
            config.setMaxConnPerRoute(Integer.parseInt(maxConnPerRoute));
        }

        config.setConnectTimeout(Integer.parseInt(PROPERTIES.getProperty("elasticsearch.connect.timeout")));
        config.setSocketTimeout(Integer.parseInt(PROPERTIES.getProperty("elasticsearch.socket.timeout")));
        config.setKeepAliveTime(Integer.parseInt(PROPERTIES.getProperty("elasticsearch.keep.alive.time")));

        // 批量操作配置
        config.setBulkFlushMaxActions(Integer.parseInt(PROPERTIES.getProperty("elasticsearch.bulk.flush.max.actions")));
        config.setBulkFlushInterval(Integer.parseInt(PROPERTIES.getProperty("elasticsearch.bulk.flush.interval")));
        config.setBulkFlushBackoffEnabled(Boolean.parseBoolean(PROPERTIES.getProperty("elasticsearch.bulk.flush.backoff.enabled")));
        config.setBulkFlushBackoffType(PROPERTIES.getProperty("elasticsearch.bulk.flush.backoff.type"));
        config.setBulkFlushBackoffDelay(Integer.parseInt(PROPERTIES.getProperty("elasticsearch.bulk.flush.backoff.delay")));
        config.setBulkFlushBackoffRetries(Integer.parseInt(PROPERTIES.getProperty("elasticsearch.bulk.flush.backoff.retries")));

        return config;
    }

    // ========================= Flink配置 =========================

    /**
     * 获取Flink配置
     *
     * @return Flink配置对象
     */
    public FlinkConfig getFlinkConfig() {
        FlinkConfig config = new FlinkConfig();

        // 检查点配置
        config.setCheckpointInterval(Long.parseLong(PROPERTIES.getProperty("flink.checkpoint.interval")));
        config.setCheckpointMinPause(Long.parseLong(PROPERTIES.getProperty("flink.checkpoint.min.pause")));
        config.setCheckpointTimeout(Long.parseLong(PROPERTIES.getProperty("flink.checkpoint.timeout")));

        // 重启策略配置
        config.setRestartAttempts(Integer.parseInt(PROPERTIES.getProperty("flink.restart.attempts")));
        config.setRestartDelaySeconds(Integer.parseInt(PROPERTIES.getProperty("flink.restart.delay.seconds")));

        return config;
    }

    // ========================= 监控指标配置 =========================

    /**
     * 获取监控指标配置
     *
     * @return 监控指标配置对象
     */
    public MetricsConfig getMetricsConfig() {
        MetricsConfig config = new MetricsConfig();

        config.setTotalEvents(PROPERTIES.getProperty("metric.total.events"));
        config.setErrorEvents(PROPERTIES.getProperty("metric.error.events"));
        config.setEsWriteFailures(PROPERTIES.getProperty("metric.es.write.failures"));

        return config;
    }

    // ========================= 验证模式配置 =========================

    /**
     * 获取JSON验证的正则表达式模式
     *
     * @return JSON验证正则表达式
     */
    public String getJsonValidationPattern() {
        return PROPERTIES.getProperty("validation.json.pattern");
    }

    /**
     * 获取运行日志验证的正则表达式模式
     *
     * @return 运行日志验证正则表达式
     */
    public String getRunLogValidationPattern() {
        return PROPERTIES.getProperty("validation.run-log.pattern");
    }
}
