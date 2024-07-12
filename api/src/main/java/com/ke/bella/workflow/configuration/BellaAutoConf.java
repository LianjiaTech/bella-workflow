package com.ke.bella.workflow.configuration;

import javax.annotation.PostConstruct;

import com.ke.bella.workflow.db.IDGenerator;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ke.bella.workflow.db.repo.InstanceRepo;
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
}
