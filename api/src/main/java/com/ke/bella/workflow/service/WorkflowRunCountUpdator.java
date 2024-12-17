package com.ke.bella.workflow.service;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ke.bella.workflow.TaskExecutor;
import com.ke.bella.workflow.db.repo.WorkflowRepo;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunShardingDB;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WorkflowRunCountUpdator {

    @Resource
    WorkflowRepo repo;

    private final Cache<String, AtomicLong> deltas = CacheBuilder.newBuilder()
            .maximumSize(10)
            .build();

    public void increase(final WorkflowRunDB wr) {
        try {
            deltas.get(wr.getWorkflowRunShardingKey(), new Callable<AtomicLong>() {
                @Override
                public AtomicLong call() throws Exception {
                    return new AtomicLong(0);
                }
            }).addAndGet(1L);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void flush() {
        deltas.asMap().forEach((k, d) -> {
            long v = d.get();
            if(v > 0) {
                LOGGER.info("update workflow run count, sharding: {}, delta: {}", k, v);
                repo.increaseShardingCount(k, v);
                d.addAndGet(-v);
            }
        });
    }

    public void trySharding() {
        try {
            WorkflowRunShardingDB sharding = repo.queryLatestWorkflowRunSharding();
            if(sharding.getCount().longValue() >= sharding.getMaxCount().longValue()) {

                LOGGER.info("new workflow_run sharding, last_key: {}", sharding.getKey());
                TaskExecutor.submit(() -> repo.newShardingTable(sharding.getKey()));
            }
        } catch (Throwable t) {
            LOGGER.info("trySharding error, t: ", t);
        }
    }
}
