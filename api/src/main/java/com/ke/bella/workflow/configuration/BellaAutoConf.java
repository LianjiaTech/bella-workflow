package com.ke.bella.workflow.configuration;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;

import javax.annotation.PostConstruct;

import com.ke.bella.workflow.db.IDGenerator;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ke.bella.workflow.db.repo.InstanceRepo;
import com.ke.bella.workflow.service.Configs;
import com.ke.infra.cloud.extension.boostrap.AppContext;
import com.ke.infra.cloud.extension.boostrap.Instance;

@Configuration
public class BellaAutoConf {
    @Autowired
    private InstanceRepo instanceRepo;

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
