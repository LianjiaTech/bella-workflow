package com.ke.bella.workflow.configuration;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;

import javax.annotation.PostConstruct;

import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ke.bella.workflow.RedisMesh;
import com.ke.bella.workflow.db.IDGenerator;
import com.ke.bella.workflow.db.repo.InstanceRepo;
import com.ke.bella.workflow.service.Configs;
import com.ke.infra.cloud.extension.boostrap.AppContext;
import com.ke.infra.cloud.extension.boostrap.Instance;

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
        Instance instance = AppContext.getInstance();
        String ip = instance.getIpAddress();
        Long id = instanceRepo.register(ip, instance.getPort());
        IDGenerator.setInstanceId(id);
    }

    @Bean
    public RedisMesh redisMesh(@Value("${bella.workflow.redis.host}") String host,
            @Value("${bella.workflow.redis.port}") int port,
            @Value("${bella.workflow.redis.user}") String user,
            @Value("${bella.workflow.redis.password}") String pwd) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setJmxNamePrefix("RedisMesh");
        JedisPool pool = new JedisPool(config, host, port, user, pwd);

        Instance instance = AppContext.getInstance();
        String key = String.format("%s:%s", instance.getIpAddress(), instance.getPort());
        RedisMesh mesh = new RedisMesh(profile, key, "bella-workflow", pool);
        return mesh;
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
}
