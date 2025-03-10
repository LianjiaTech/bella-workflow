package com.ke.bella.workflow;

import com.ke.bella.job.queue.worker.JobQueueWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import com.ke.bella.openapi.server.BellaServerContextHolder;
import com.ke.bella.workflow.db.repo.InstanceRepo;

import java.util.Objects;

@Component
public class ShutdownListener implements ApplicationListener<ApplicationEvent> {

    @Autowired
    InstanceRepo repo;

    @Autowired
    RedisMesh redisMesh;

    @Autowired(required = false)
    JobQueueWorker jobQueueWorker;

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if(event instanceof ContextClosedEvent) {
            if(Objects.nonNull(jobQueueWorker)) {
                jobQueueWorker.stop();
            }
            redisMesh.shutdown();
            repo.unregister(BellaServerContextHolder.getIp(), BellaServerContextHolder.getPort());
        }
    }
}
