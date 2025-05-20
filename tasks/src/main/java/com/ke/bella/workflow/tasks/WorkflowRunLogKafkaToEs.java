package com.ke.bella.workflow.tasks;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.shaded.guava31.com.google.common.base.Throwables;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.streaming.connectors.elasticsearch7.ElasticsearchSink;
import org.apache.flink.streaming.connectors.elasticsearch7.RestClientFactory;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ke.bella.workflow.tasks.config.AppConfig;
import com.ke.bella.workflow.tasks.config.ElasticsearchConfig;
import com.ke.bella.workflow.tasks.config.FlinkConfig;
import com.ke.bella.workflow.tasks.config.KafkaConfig;
import com.ke.bella.workflow.tasks.config.MetricsConfig;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

public class WorkflowRunLogKafkaToEs {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowRunLogKafkaToEs.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 常量定义 - 将在运行时从配置文件中获取
    private static String JOB_NAME;
    private static String ERROR_STREAM_NAME;

    // 定义侧输出流标签，用于处理错误数据
    private static OutputTag<String> ERROR_OUTPUT_TAG;

    // 定义监控指标名称 - 将在运行时从配置文件中获取
    private static String METRIC_TOTAL_EVENTS;
    private static String METRIC_ERROR_EVENTS;
    private static String METRIC_ES_WRITE_FAILURES;

    /**
     * 应用程序入口点
     *
     * @param args 命令行参数
     * 
     * @throws Exception 如果作业执行失败
     */
    public static void main(String[] args) throws Exception {
        // 解析命令行参数
        final ParameterTool parameterTool = ParameterTool.fromArgs(args);

        // 获取配置
        AppConfig config = AppConfig.getInstance(parameterTool.getProperties());

        // 初始化常量
        JOB_NAME = config.getJobName();
        ERROR_STREAM_NAME = config.getErrorStreamName();
        ERROR_OUTPUT_TAG = new OutputTag<String>(ERROR_STREAM_NAME) {
        };

        // 获取监控指标名称
        MetricsConfig metricsConfig = config.getMetricsConfig();
        METRIC_TOTAL_EVENTS = metricsConfig.getTotalEvents();
        METRIC_ERROR_EVENTS = metricsConfig.getErrorEvents();
        METRIC_ES_WRITE_FAILURES = metricsConfig.getEsWriteFailures();

        // 设置执行环境
        final StreamExecutionEnvironment env = configureEnvironment(parameterTool, config);

        // 创建并配置Kafka数据源
        DataStream<String> kafkaStream = createKafkaSource(env, parameterTool, config);

        // 创建数据处理流水线
        SingleOutputStreamOperator<String> processedStream = processKafkaStream(kafkaStream, parameterTool, config, env);

        // 处理错误数据
        handleErrorStream(processedStream);

        // 按workflowId分组并写入Elasticsearch
        writeToElasticsearch(processedStream, parameterTool, config, env);

        // 执行作业
        env.execute(JOB_NAME);
    }

    /**
     * 配置Flink执行环境
     *
     * @param parameterTool 命令行参数工具
     * @param config        应用配置
     * 
     * @return 配置好的执行环境
     */
    private static StreamExecutionEnvironment configureEnvironment(
            ParameterTool parameterTool, AppConfig config) {

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 设置全局并行度（如果通过命令行参数指定）
        if(parameterTool.has("parallelism")) {
            LOGGER.info("Setting parallelism to {}", parameterTool.getInt("parallelism"));
            env.setParallelism(parameterTool.getInt("parallelism"));
        }

        // 注册全局参数，使其可以在算子函数中访问
        env.getConfig().setGlobalJobParameters(parameterTool);

        // 获取Flink配置
        FlinkConfig flinkConfig = config.getFlinkConfig();

        // 配置检查点以确保 EXACTLY_ONCE 语义
        env.enableCheckpointing(flinkConfig.getCheckpointInterval(), CheckpointingMode.EXACTLY_ONCE);

        // 配置检查点的高级选项
        CheckpointConfig cpConfig = env.getCheckpointConfig();
        cpConfig.setMinPauseBetweenCheckpoints(flinkConfig.getCheckpointMinPause());
        cpConfig.setCheckpointTimeout(flinkConfig.getCheckpointTimeout());
        cpConfig.enableExternalizedCheckpoints(
                CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);

        // 配置重启策略
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(
                flinkConfig.getRestartAttempts(),
                Time.of(flinkConfig.getRestartDelaySeconds(), TimeUnit.SECONDS)));

        return env;
    }

    /**
     * 创建并配置Kafka数据源
     *
     * @param env           执行环境
     * @param parameterTool 命令行参数工具
     * @param config        应用配置
     * 
     * @return Kafka数据流
     */
    private static DataStream<String> createKafkaSource(
            StreamExecutionEnvironment env, ParameterTool parameterTool, AppConfig config) {

        // 获取Kafka配置
        KafkaConfig kafkaConfig = config.getKafkaConfig();

        // 创建Kafka消费者
        FlinkKafkaConsumer<String> kafkaConsumer = new FlinkKafkaConsumer<>(
                kafkaConfig.getTopic(),
                new SimpleStringSchema(),
                kafkaConfig.toProperties());

        // 设置消费起始位置
        kafkaConsumer.setStartFromGroupOffsets();
        kafkaConsumer.setCommitOffsetsOnCheckpoints(true);

        // 添加水印策略（根据处理时间）
        return env
                .addSource(kafkaConsumer)
                .name("Kafka-Source")
                .uid("kafka-source") // 为了状态恢复时的一致性
                .setParallelism(parameterTool.getInt("source.parallelism", env.getParallelism()));
    }

    /**
     * 处理Kafka数据流
     *
     * @param kafkaStream   Kafka数据流
     * @param parameterTool 命令行参数工具
     * @param config        应用配置
     * @param env           执行环境
     * 
     * @return 处理后的数据流
     */
    private static SingleOutputStreamOperator<String> processKafkaStream(
            DataStream<String> kafkaStream, ParameterTool parameterTool,
            AppConfig config, StreamExecutionEnvironment env) {

        // 使用配置的JSON验证正则表达式
        String regex = config.getJsonValidationPattern();
        String runLogRegex = config.getRunLogValidationPattern();
        Pattern runLogPattern = Pattern.compile(runLogRegex);
        Pattern infoMsgPattern = Pattern.compile(regex);

        // 数据预处理，包括错误处理和监控
        return kafkaStream
                .process(new LogProcessFunction(runLogPattern, infoMsgPattern))
                .name("Data-Validation-And-Transformation")
                .uid("data-validation")
                .setParallelism(parameterTool.getInt("process.parallelism", env.getParallelism()));
    }

    /**
     * 处理错误数据流
     *
     * @param processedStream 处理后的主数据流
     */
    private static void handleErrorStream(SingleOutputStreamOperator<String> processedStream) {
        DataStream<String> errorStream = processedStream.getSideOutput(ERROR_OUTPUT_TAG);
        errorStream
                .map(error -> "Error processing record: " + error)
                .print()
                .name("Error-Logger")
                .setParallelism(1);
    }

    /**
     * 按workflowId分组并写入Elasticsearch
     *
     * @param processedStream 处理后的数据流
     * @param parameterTool   命令行参数工具
     * @param config          应用配置
     * @param env             执行环境
     */
    private static void writeToElasticsearch(
            SingleOutputStreamOperator<String> processedStream,
            ParameterTool parameterTool, AppConfig config,
            StreamExecutionEnvironment env) {

        // 按workflowId分组，确保相关数据由同一任务处理
        SingleOutputStreamOperator<String> keyedStream = processedStream
                .map(new WorkflowIdExtractor())
                .name("Workflow-Id-Extractor")
                .uid("workflow-id-extractor")
                .keyBy(tuple -> tuple.f0) // 按workflowId分组
                .map(tuple -> tuple.f1)   // 只保留JSON字符串
                .name("Workflow-Keyed-Stream")
                .uid("keyed-stream");

        // 创建并配置Elasticsearch Sink
        ElasticsearchSink<String> esSink = createElasticsearchSink(config);

        // 添加Elasticsearch Sink
        keyedStream
                .addSink(esSink)
                .name("Elasticsearch-Sink")
                .uid("es-sink")
                .setParallelism(parameterTool.getInt("sink.parallelism", env.getParallelism()));
    }

    /**
     * 创建并配置Elasticsearch Sink
     *
     * @param config 应用配置
     * 
     * @return 配置好的Elasticsearch Sink
     */
    private static ElasticsearchSink<String> createElasticsearchSink(AppConfig config) {
        // 获取Elasticsearch配置
        ElasticsearchConfig esConfig = config.getElasticsearchConfig();

        // 构建HttpHost列表
        List<HttpHost> httpHosts = new ArrayList<>();
        for (String host : esConfig.getHosts()) {
            String[] parts = host.split(":");
            httpHosts.add(new HttpHost(parts[0], Integer.parseInt(parts[1])));
        }

        // 创建ES Sink
        ElasticsearchSink.Builder<String> esSinkBuilder = new ElasticsearchSink.Builder<>(
                httpHosts,
                new ElasticSearchSinkFunctionImpl(esConfig.getIndexTemplate()));

        // 配置ES Sink
        esSinkBuilder.setBulkFlushMaxActions(esConfig.getBulkFlushMaxActions());
        esSinkBuilder.setBulkFlushInterval(esConfig.getBulkFlushInterval());
        esSinkBuilder.setBulkFlushBackoff(esConfig.isBulkFlushBackoffEnabled());
        esSinkBuilder.setBulkFlushBackoffType(ElasticsearchSink.FlushBackoffType.valueOf(esConfig.getBulkFlushBackoffType()));
        esSinkBuilder.setBulkFlushBackoffDelay(esConfig.getBulkFlushBackoffDelay());
        esSinkBuilder.setBulkFlushBackoffRetries(esConfig.getBulkFlushBackoffRetries());

        // 配置认证信息
        esSinkBuilder.setRestClientFactory(
                new SerializableRestClientFactory(
                        esConfig.getUsername(),
                        esConfig.getPassword(),
                        esConfig.getMaxConnTotal(),
                        esConfig.getMaxConnPerRoute(),
                        esConfig.getConnectTimeout(),
                        esConfig.getSocketTimeout(),
                        esConfig.getKeepAliveTime()));

        return esSinkBuilder.build();
    }

    /**
     * 实现ElasticsearchSinkFunction接口的静态内部类，确保可序列化
     */
    private static class ElasticSearchSinkFunctionImpl implements ElasticsearchSinkFunction<String> {
        private static final long serialVersionUID = 1L;
        private final String indexTemplate;
        private transient Counter esWriteFailures;

        public ElasticSearchSinkFunctionImpl(String indexTemplate) {
            this.indexTemplate = indexTemplate;
        }

        @Override
        public void process(String element, RuntimeContext ctx, RequestIndexer indexer) {
            try {
                // 初始化指标计数器（如果需要）
                if(esWriteFailures == null) {
                    esWriteFailures = ctx.getMetricGroup().counter(METRIC_ES_WRITE_FAILURES);
                }

                LOGGER.debug("Processing element for Elasticsearch: {}", element);
                String dateFormat = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                String elasticsearchIndex = String.format(indexTemplate, dateFormat);

                IndexRequest request = Requests.indexRequest()
                        .index(elasticsearchIndex)
                        .source(element, XContentType.JSON);

                indexer.add(request);
            } catch (Exception e) {
                if(esWriteFailures != null) {
                    esWriteFailures.inc();
                }
                // 记录更详细的错误信息
                LOGGER.error("Failed to process workflow run log: {}, error: {}",
                        element, Throwables.getStackTraceAsString(e));
            }
        }
    }

    /**
     * 日志处理函数，负责验证和转换数据
     */
    private static class LogProcessFunction extends ProcessFunction<String, String> {
        private final Pattern runLogPattern;
        private final Pattern infoMsgPattern;
        private transient Counter totalEvents;
        private transient Counter errorEvents;

        public LogProcessFunction(Pattern runLogPattern, Pattern infoMsgPattern) {
            this.runLogPattern = runLogPattern;
            this.infoMsgPattern = infoMsgPattern;
        }

        @Override
        public void open(Configuration parameters) {
            // 初始化指标计数器
            totalEvents = getRuntimeContext().getMetricGroup().counter(METRIC_TOTAL_EVENTS);
            errorEvents = getRuntimeContext().getMetricGroup().counter(METRIC_ERROR_EVENTS);
        }

        @Override
        public void processElement(String value, Context ctx, Collector<String> out) {
            totalEvents.inc();
            try {
                LOGGER.debug("Processing log: {}", value);

                // 验证是否为运行日志
                if(!isRunLog(value)) {
                    return;
                }

                // 验证JSON格式
                if(!isValidJson(value)) {
                    handleInvalidFormat(value, ctx);
                    return;
                }

                // 处理日志
                processLog(value, ctx, out);

            } catch (Exception e) {
                handleProcessingError(value, ctx, e);
            }
        }

        /**
         * 验证是否为运行日志
         */
        private boolean isRunLog(String value) {
            Matcher runLogMatcher = runLogPattern.matcher(value);
            if(!runLogMatcher.find()) {
                LOGGER.info("Not a run log: {}", value);
                return false;
            }
            return true;
        }

        /**
         * 验证JSON格式
         */
        private boolean isValidJson(String value) {
            Matcher matcher = infoMsgPattern.matcher(value);
            return matcher.find();
        }

        /**
         * 处理无效格式的日志
         */
        private void handleInvalidFormat(String value, Context ctx) {
            LOGGER.warn("Invalid log format: {}", value);
            errorEvents.inc();
            ctx.output(ERROR_OUTPUT_TAG, String.format("Invalid log format: %s", value));
        }

        /**
         * 处理日志
         */
        private void processLog(String value, Context ctx, Collector<String> out) throws Exception {
            Matcher matcher = infoMsgPattern.matcher(value);
            if(matcher.find()) {
                JsonNode jsonNode = OBJECT_MAPPER.readTree(matcher.group());

                // 处理不同格式的日志
                if(!jsonNode.has("info_msg") || jsonNode.get("info_msg").isNull()) {
                    processDirectFormat(jsonNode, value, ctx, out);
                } else {
                    processStandardFormat(jsonNode, value, ctx, out);
                }
            }
        }

        /**
         * 处理直接格式的日志（没有info_msg字段）
         */
        private void processDirectFormat(JsonNode jsonNode, String value, Context ctx, Collector<String> out) {
            try {
                WorkflowRunLog workflowRunLogNew = OBJECT_MAPPER.convertValue(jsonNode, WorkflowRunLog.class);
                WorkflowRunLogEs logEsNew = WorkflowRunLog.transfer(workflowRunLogNew);
                if(logEsNew != null) {
                    out.collect(OBJECT_MAPPER.writeValueAsString(logEsNew));
                } else {
                    errorEvents.inc();
                    ctx.output(ERROR_OUTPUT_TAG, String.format("Failed to transfer log: %s", value));
                }
            } catch (Exception e) {
                errorEvents.inc();
                ctx.output(ERROR_OUTPUT_TAG, String.format("Invalid log, missing info_msg field: %s, error: %s",
                        value, Throwables.getStackTraceAsString(e)));
            }
        }

        /**
         * 处理标准格式日志（包含info_msg字段）
         */
        private void processStandardFormat(JsonNode jsonNode, String value, Context ctx, Collector<String> out) {
            try {
                WorkflowRunLog runLog = OBJECT_MAPPER.readValue(jsonNode.get("info_msg").asText(), WorkflowRunLog.class);
                WorkflowRunLogEs workflowRunLogEs = WorkflowRunLog.transfer(runLog);

                if(workflowRunLogEs != null) {
                    out.collect(OBJECT_MAPPER.writeValueAsString(workflowRunLogEs));
                } else {
                    errorEvents.inc();
                    ctx.output(ERROR_OUTPUT_TAG, String.format("Failed to transfer log: %s", value));
                }
            } catch (Exception e) {
                errorEvents.inc();
                ctx.output(ERROR_OUTPUT_TAG, String.format("Failed to parse info_msg: %s, error: %s",
                        value, Throwables.getStackTraceAsString(e)));
            }
        }

        /**
         * 处理处理过程中的错误
         */
        private void handleProcessingError(String value, Context ctx, Exception e) {
            errorEvents.inc();
            LOGGER.error("Failed to process log: {}, error: {}", value, Throwables.getStackTraceAsString(e));
            ctx.output(ERROR_OUTPUT_TAG, String.format("Failed to process log: %s, error: %s",
                    value, Throwables.getStackTraceAsString(e)));
        }
    }

    /**
     * 提取workflowId用于数据分组
     */
    private static class WorkflowIdExtractor implements MapFunction<String, Tuple2<String, String>> {
        @Override
        public org.apache.flink.api.java.tuple.Tuple2<String, String> map(String value) throws Exception {
            try {
                JsonNode jsonNode = OBJECT_MAPPER.readTree(value);
                String workflowId = jsonNode.has("workflowId") ? jsonNode.get("workflowId").asText() : "default";
                return new org.apache.flink.api.java.tuple.Tuple2<>(workflowId, value);
            } catch (Exception e) {
                LOGGER.warn("Failed to extract workflowId, using default: {}", e.getMessage());
                return new org.apache.flink.api.java.tuple.Tuple2<>("default", value);
            }
        }
    }

    /**
     * 可序列化的Elasticsearch REST客户端工厂
     */
    private static class SerializableRestClientFactory implements RestClientFactory {
        private static final long serialVersionUID = 1L;
        private final String username;
        private final String password;
        private final int elasticsearchMaxConnTotal;
        private final int elasticsearchMaxConnPerRoute;
        private final int connectTimeout;
        private final int socketTimeout;
        private final int keepAliveTime;

        public SerializableRestClientFactory(String username, String password, int elasticsearchMaxConnTotal,
                int elasticsearchMaxConnPerRoute, int connectTimeout, int socketTimeout,
                int keepAliveTime) {
            this.username = username;
            this.password = password;
            this.elasticsearchMaxConnTotal = elasticsearchMaxConnTotal;
            this.elasticsearchMaxConnPerRoute = elasticsearchMaxConnPerRoute;
            this.connectTimeout = connectTimeout;
            this.socketTimeout = socketTimeout;
            this.keepAliveTime = keepAliveTime;
        }

        @Override
        public void configureRestClientBuilder(RestClientBuilder restClientBuilder) {
            restClientBuilder.setHttpClientConfigCallback(this::customizeHttpClient);

            // 设置请求超时配置
            restClientBuilder.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                    .setConnectTimeout(connectTimeout)
                    .setSocketTimeout(socketTimeout));
        }

        private HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
            return httpClientBuilder
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .setMaxConnTotal(elasticsearchMaxConnTotal)
                    .setMaxConnPerRoute(elasticsearchMaxConnPerRoute)
                    .setKeepAliveStrategy((response, context) -> keepAliveTime);
        }
    }

    /**
     * 工作流运行日志数据模型
     */
    @Data
    @NoArgsConstructor
    public static class WorkflowRunLog {
        private String bellaTraceId;
        private String akCode;
        private String event;
        private String tenantId;
        private Long userId;
        private String userName;
        private String workflowId;
        private String workflowRunId;
        private int flashMode;
        private String triggerFrom;
        private String threadId;
        private boolean stateful;
        private Object sys;
        private Object inputs;
        private Object outputs;
        private String status;
        private Long ctime;
        private Long elapsedTime;
        private String nodeId;
        private String nodeType;
        private String nodeTitle;
        private String nodeRunId;
        private Object nodeInputs;
        private Object nodeProcessData;
        private Object nodeOutputs;
        private String error;
        private boolean iteration;
        private Integer iterationIndex;

        /**
         * 将WorkflowRunLog转换为WorkflowRunLogEs
         *
         * @param runLog 源日志对象
         * 
         * @return 转换后的ES日志对象，失败时返回null
         */
        public static WorkflowRunLogEs transfer(WorkflowRunLog runLog) {
            try {
                String sys = runLog.getSys() == null ? "" : OBJECT_MAPPER.writeValueAsString(runLog.getSys());
                String inputs = runLog.getInputs() == null ? "" : OBJECT_MAPPER.writeValueAsString(runLog.getInputs());
                String outputs = runLog.getOutputs() == null ? "" : OBJECT_MAPPER.writeValueAsString(runLog.getOutputs());
                String nodeInputs = runLog.getNodeInputs() == null ? "" : OBJECT_MAPPER.writeValueAsString(runLog.getNodeInputs());
                String nodeProcessData = runLog.getNodeProcessData() == null ? "" : OBJECT_MAPPER.writeValueAsString(runLog.getNodeProcessData());
                String nodeOutputs = runLog.getNodeOutputs() == null ? "" : OBJECT_MAPPER.writeValueAsString(runLog.getNodeOutputs());

                return WorkflowRunLogEs.builder()
                        .bellaTraceId(runLog.getBellaTraceId())
                        .akCode(runLog.getAkCode())
                        .event(runLog.getEvent())
                        .tenantId(runLog.getTenantId())
                        .userId(runLog.getUserId())
                        .userName(runLog.getUserName())
                        .workflowId(runLog.getWorkflowId())
                        .workflowRunId(runLog.getWorkflowRunId())
                        .flashMode(runLog.getFlashMode())
                        .triggerFrom(runLog.getTriggerFrom())
                        .threadId(runLog.getThreadId())
                        .stateful(runLog.isStateful())
                        .sys(sys)
                        .inputs(inputs)
                        .outputs(outputs)
                        .status(runLog.getStatus())
                        .ctime(runLog.getCtime())
                        .elapsedTime(runLog.getElapsedTime())
                        .nodeId(runLog.getNodeId())
                        .nodeType(runLog.getNodeType())
                        .nodeTitle(runLog.getNodeTitle())
                        .nodeRunId(runLog.getNodeRunId())
                        .nodeInputs(nodeInputs)
                        .nodeProcessData(nodeProcessData)
                        .nodeOutputs(nodeOutputs)
                        .error(runLog.getError())
                        .iteration(runLog.isIteration())
                        .iterationIndex(runLog.getIterationIndex())
                        .build();
            } catch (Exception e) {
                LOGGER.error("Failed to transfer WorkflowRunLog to WorkflowRunLogEs, error: {}", Throwables.getStackTraceAsString(e));
                return null;
            }
        }
    }

    /**
     * Elasticsearch存储的工作流运行日志数据模型
     */
    @Data
    @NoArgsConstructor
    @SuperBuilder(toBuilder = true)
    public static class WorkflowRunLogEs {
        private String bellaTraceId;
        private String akCode;
        private String event;
        private String tenantId;
        private Long userId;
        private String userName;
        private String workflowId;
        private String workflowRunId;
        private int flashMode;
        private String triggerFrom;
        private String threadId;
        private boolean stateful;
        private String sys;
        private String inputs;
        private String outputs;
        private String status;
        private Long ctime;
        private Long elapsedTime;
        private String nodeId;
        private String nodeType;
        private String nodeTitle;
        private String nodeRunId;
        private String nodeInputs;
        private String nodeProcessData;
        private String nodeOutputs;
        private String error;
        private boolean iteration;
        private Integer iterationIndex;
    }
}
