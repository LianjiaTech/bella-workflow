package com.ke.bella.workflow.trigger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Throwables;
import com.ke.bella.workflow.TaskExecutor.NamedThreadFactory;
import com.ke.bella.workflow.db.repo.InstanceRepo;
import com.ke.bella.workflow.db.tables.pojos.TenantDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowKafkaTriggerDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowSchedulingDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowWebotTriggerDB;
import com.ke.bella.workflow.service.WorkflowClient;
import com.ke.bella.workflow.service.WorkflowService;
import com.ke.bella.workflow.service.WorkflowTriggerService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WorkflowSchedulingTriggerHelper {
    private static final ThreadFactory TRIGGER_THREAD_FACTORY = new NamedThreadFactory("workflow-scheduling-trigger", true);
    private static final Integer BATCH_SIZE = 100;

    @Autowired
    WorkflowTriggerService ws;

    @Autowired
    InstanceRepo instanceRepo;

    @Autowired
    WorkflowClient workflowClient;

    @Autowired
    WorkflowService wfs;

    private ThreadPoolExecutor triggerPool = null;

    public void start() {
        triggerPool = new ThreadPoolExecutor(10, 50, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000),
                TRIGGER_THREAD_FACTORY, new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public void stop() {
        triggerPool.shutdownNow();
    }

    @Transactional
    @SuppressWarnings("all")
    public int trySchedulingTrigger() {
        // try lock to scheduling workflow_scheduling
        instanceRepo.forUpdateInstance1();
        // list all pending task which trigger time is before current
        List<WorkflowSchedulingDB> pendingScheduling = ws.listPendingTask(LocalDateTime.now(), BATCH_SIZE);
        List<String> tenantIds = pendingScheduling.stream().map(WorkflowSchedulingDB::getTenantId).collect(Collectors.toList());
        Map<String, TenantDB> tenantsMap = getTenantsMap(tenantIds);
        if(!CollectionUtils.isEmpty(pendingScheduling)) {
            for (WorkflowSchedulingDB workflowScheduling : pendingScheduling) {
                TenantDB tenantDB = tenantsMap.get(workflowScheduling.getTenantId());
                // "helper" thread to refresh trigger next time
                ws.refreshTriggerNextTime(workflowScheduling);
                CompletableFuture.runAsync(() -> {
                    workflowClient.workflowRun(tenantDB, workflowScheduling);
                }, triggerPool).exceptionally(e -> {
                    LOGGER.error("workflow scheduling error, e: {}", Throwables.getStackTraceAsString(e));
                    return null;
                });
            }
        }
        return pendingScheduling.size();
    }

    public void tryKafkaTrigger(List<WorkflowKafkaTriggerDB> dbs, Object event) {
        List<String> tenantIds = dbs.stream().map(WorkflowKafkaTriggerDB::getTenantId).collect(Collectors.toList());
        Map<String, TenantDB> tenantsMap = getTenantsMap(tenantIds);
        if(!CollectionUtils.isEmpty(dbs)) {
            for (WorkflowKafkaTriggerDB t : dbs) {
                TenantDB tenantDB = tenantsMap.get(t.getTenantId());
                CompletableFuture.runAsync(() -> {
                    workflowClient.runWorkflow(tenantDB, t, event);
                }, triggerPool).exceptionally(e -> {
                    LOGGER.error("workflow trigger error, e: {}", Throwables.getStackTraceAsString(e));
                    return null;
                });
            }
        }
    }

    public void tryWebotTrigger(List<WorkflowWebotTriggerDB> dbs, Object event) {
        List<String> tenantIds = dbs.stream().map(WorkflowWebotTriggerDB::getTenantId).collect(Collectors.toList());
        Map<String, TenantDB> tenantsMap = getTenantsMap(tenantIds);
        if(!CollectionUtils.isEmpty(dbs)) {
            for (WorkflowWebotTriggerDB t : dbs) {
                TenantDB tenantDB = tenantsMap.get(t.getTenantId());
                CompletableFuture.runAsync(() -> {
                    workflowClient.runWorkflow(tenantDB, t, event);
                }, triggerPool).exceptionally(e -> {
                    LOGGER.error("workflow trigger error, e: {}", Throwables.getStackTraceAsString(e));
                    return null;
                });
            }
        }
    }

    @NotNull
    private Map<String, TenantDB> getTenantsMap(List<String> tenantIds) {
        List<TenantDB> tenantDbs = wfs.listTenants(tenantIds);
        return tenantDbs.stream().collect(Collectors.toMap(TenantDB::getTenantId, Function.identity(), (k1, k2) -> k1));
    }
}
