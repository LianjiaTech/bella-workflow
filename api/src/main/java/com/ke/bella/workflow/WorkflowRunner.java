package com.ke.bella.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.LockSupport;

import com.google.common.base.Throwables;
import com.ke.bella.workflow.WorkflowRunState.WorkflowRunStatus;
import com.ke.bella.workflow.node.BaseNode;

import ch.qos.logback.classic.Logger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkflowRunner {

    public void run(WorkflowContext context, IWorkflowCallback callback) {
        context.start();
        callback.onWorkflowRunStarted(context);
        run0(context, callback);
    }

    @SuppressWarnings("rawtypes")
    public void runNode(WorkflowContext context, IWorkflowCallback callback, String nodeId) {
        BaseNode node = context.getNode(nodeId);
        try {
            node.run(context, callback);
        } catch (Exception e) {
            LOGGER.info("node run failed, e: {}", Throwables.getStackTraceAsString(e));
            // single node does not require processing, swallow the exception
        }
    }

    public void resume(WorkflowContext context, IWorkflowCallback callback, List<String> nodeIds) {
        context.getState().setStatus(WorkflowRunStatus.running);
        context.getState().putNextNodes(nodeIds);
        callback.onWorkflowRunResumed(context);
        run0(context, callback);
    }

    @SuppressWarnings("rawtypes")
    private void run0(WorkflowContext context, IWorkflowCallback callback) {
        try {
            context.validate();

            final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());
            List<CompletableFuture> fs = new ArrayList<>();
            while (!context.isFinish()) {
                List<BaseNode> nodes = context.getNextNodes();
                if(!nodes.isEmpty()) {
                    for (BaseNode node : nodes) {
                        CompletableFuture future = null;
                        if(context.isResume(node.getNodeId())) {
                            future = TaskExecutor.submit(() -> node.resume(context, callback)).exceptionally(e -> {
                                exceptions.add(e);
                                return null;
                            });
                        } else {
                            future = TaskExecutor.submit(() -> node.run(context, callback)).exceptionally(e -> {
                                exceptions.add(e);
                                return null;
                            });
                        }
                        fs.add(future);
                    }
                } else {
                    LockSupport.parkNanos(1000000); // 1ms
                }
            }

            CompletableFuture.allOf(fs.toArray(new CompletableFuture[0])).join();

            if(!exceptions.isEmpty()) {
                throw exceptions.get(0);
            }

            if(context.isSuspended()) {
                context.getState().setStatus(WorkflowRunStatus.suspended);
                callback.onWorkflowRunSuspended(context);
            } else {
                context.getState().setStatus(WorkflowRunStatus.succeeded);
                callback.onWorkflowRunSucceeded(context);
            }
        } catch (Throwable e) {
            LOGGER.info(e.getMessage(), e);
            context.getState().setStatus(WorkflowRunStatus.failed);
            callback.onWorkflowRunFailed(context, e.toString(), e);
        }
    }
}
