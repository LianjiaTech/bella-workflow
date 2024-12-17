package com.ke.bella.workflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import com.ke.bella.workflow.db.repo.InstanceRepo;
import com.ke.infra.cloud.extension.boostrap.AppContext;
import com.ke.infra.cloud.extension.boostrap.Instance;

@Component
public class ShutdownListener implements ApplicationListener<ApplicationEvent> {

    @Autowired
    InstanceRepo repo;

    @Autowired
    RedisMesh redisMesh;

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if(event instanceof ContextClosedEvent) {
            redisMesh.shutdown();
            Instance instance = AppContext.getInstance();
            repo.unregister(instance.getIpAddress(), instance.getPort());
        }
    }
}
