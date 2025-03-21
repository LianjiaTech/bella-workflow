package com.ke.bella.workflow.configuration;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.cache.CacheBuilder;
import com.ke.bella.job.queue.worker.JobQueueWorker;
import com.ke.bella.openapi.server.BellaServerContextHolder;
import com.ke.bella.workflow.RedisMesh;
import com.ke.bella.workflow.db.IDGenerator;
import com.ke.bella.workflow.db.repo.InstanceRepo;
import com.ke.bella.workflow.service.Configs;
import com.ke.bella.workflow.service.WorkflowService;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class BellaAutoConf {
    @Autowired
    private InstanceRepo instanceRepo;

    @Value("${spring.profiles.active}")
    String profile;

    @Bean
    public GroupedOpenApi apis() {
        return GroupedOpenApi.builder()
                .group("all")
                .pathsToMatch("/**")
                .build();
    }

    @PostConstruct
    public void registerInstance() {
        String ip = BellaServerContextHolder.getIp();
        Long id = instanceRepo.register(ip, BellaServerContextHolder.getPort());
        IDGenerator.setInstanceId(id);
    }

    @Bean
    public RedisMesh redisMesh(@Value("${bella.workflow.redis.host}") String host,
            @Value("${bella.workflow.redis.port}") int port,
            @Value("${bella.workflow.redis.user:#{null}}") String user,
            @Value("${bella.workflow.redis.password}") String pwd) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setJmxNamePrefix("RedisMesh");
        JedisPool pool = new JedisPool(config, host, port, user, pwd);

        String key = String.format("%s:%s", BellaServerContextHolder.getIp(), BellaServerContextHolder.getPort());
        RedisMesh mesh = new RedisMesh(profile, key, "bella-workflow", pool);
        mesh.start();
        return mesh;
    }

    @Bean
    @ConditionalOnProperty(name = "bella.job-queue.worker.enabled", havingValue = "true")
    public JobQueueWorker jobQueueWorker(@Value("${bella.job-queue.url}") String url,
            @Value("${bella.job-queue.worker.endpoint}") String endpoint,
            @Value("${bella.job-queue.worker.queueName}") String queueName, WorkflowService ws) {
        JobQueueWorker jobQueueWorker = new JobQueueWorker(url, endpoint, queueName);
        jobQueueWorker.setTaskHandler(task -> ws.runWorkflow(task, CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build()));
        jobQueueWorker.start();
        return jobQueueWorker;
    }

    @PostConstruct
    public void setThreadAllocatedMemoryTracable() {
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        if(threadMxBean == null) {
            return;
        }

        try {
            Method m1 = threadMxBean.getClass().getMethod("isThreadAllocatedMemorySupported");
            if(m1 != null) {
                m1.setAccessible(true);
                Boolean support = (Boolean) m1.invoke(threadMxBean);
                if(support != null && support.booleanValue()) {
                    Method m2 = threadMxBean.getClass().getMethod("setThreadAllocatedMemoryEnabled", boolean.class);
                    m2.setAccessible(true);
                    m2.invoke(threadMxBean, true);
                    Configs.isThreadAllocatedMemorySupported = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Bean
    public RestHighLevelClient restHighLevelClient(@Value("${bella.workflow.run-log.elasticsearch.hosts}") String hosts,
            @Value("${bella.workflow.run-log.elasticsearch.username}") String username,
            @Value("${bella.workflow.run-log.elasticsearch.password}") String password) {
        HttpHost[] hostArray = Arrays.stream(hosts.split(",")).map(HttpHost::create).toArray(HttpHost[]::new);
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));
        return new RestHighLevelClient(
                RestClient.builder(hostArray).setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider)));
    }
}
