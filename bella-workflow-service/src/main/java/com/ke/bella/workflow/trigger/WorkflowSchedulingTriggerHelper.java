package com.ke.bella.workflow.trigger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Throwables;
import com.ke.bella.workflow.TaskExecutor.NamedThreadFactory;
import com.ke.bella.workflow.db.repo.InstanceRepo;
import com.ke.bella.workflow.db.tables.pojos.WorkflowSchedulingDB;
import com.ke.bella.workflow.service.WorkflowClient;
import com.ke.bella.workflow.service.WorkflowSchedulingService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WorkflowSchedulingTriggerHelper {
    private static final ThreadFactory TRIGGER_THREAD_FACTORY = new NamedThreadFactory("workflow-scheduling-trigger", true);
    private static final Integer BATCH_SIZE = 100;

    @Autowired
    WorkflowSchedulingService ws;

    @Autowired
    InstanceRepo instanceRepo;

    @Autowired
    WorkflowClient workflowClient;

    private ThreadPoolExecutor triggerPool = null;

    public void start() {
        triggerPool = new ThreadPoolExecutor(10, 50, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000),
                TRIGGER_THREAD_FACTORY);
    }

    public void stop() {
        triggerPool.shutdownNow();
    }

    @Transactional
    @SuppressWarnings("all")
    public int tryTrigger() {
        // try lock to scheduling workflow_scheduling
        instanceRepo.forUpdateInstance1();
        // list all pending task which trigger time is before current
        List<WorkflowSchedulingDB> pendingScheduling = ws.listPendingTask(LocalDateTime.now(), BATCH_SIZE);
        if(!CollectionUtils.isEmpty(pendingScheduling)) {
            for (WorkflowSchedulingDB workflowScheduling : pendingScheduling) {
                // "helper" thread to refresh trigger next time
                ws.refreshTriggerNextTime(workflowScheduling);
                CompletableFuture.runAsync(() -> {
                    workflowClient.workflowRun(workflowScheduling);
                }, triggerPool).exceptionally(e -> {
                    LOGGER.error("workflow scheduling error, e: {}", Throwables.getStackTraceAsString(e));
                    return null;
                });
            }
        }
        return pendingScheduling.size();
    }
}
