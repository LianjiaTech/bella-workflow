package com.ke.bella.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

import com.google.common.base.Throwables;
import com.ke.bella.workflow.WorkflowRunState.WorkflowRunStatus;
import com.ke.bella.workflow.node.BaseNode;
import com.ke.bella.workflow.utils.Utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkflowRunner {
    private static final Map<String, WorkflowContext> runnningContexts = new ConcurrentHashMap<>();

    public static boolean isRunning(String runId) {
        return runnningContexts.containsKey(runId);
    }

    public static void interrupt(String runId) {
        WorkflowContext ctx = runnningContexts.get(runId);
        if(ctx != null) {
            ctx.setInterrupted(true);
        }
    }

    public void run(WorkflowContext context, IWorkflowCallback callback) {
        try {
            addRunningContext(context);
            context.start();
            callback.onWorkflowRunStarted(context);
            context.validate();
            run0(context, callback);
        } catch (Throwable e) {
            LOGGER.info(e.getMessage(), e);
            context.getState().setStatus(WorkflowRunStatus.failed);
            callback.onWorkflowRunFailed(context, e.toString(), e);
        } finally {
            removeRunningContext(context);
        }
    }

    @SuppressWarnings("rawtypes")
    public void runNode(WorkflowContext context, IWorkflowCallback callback, String nodeId) {
        BaseNode node = context.getNode(nodeId);
        try {
            addRunningContext(context);
            WorkflowSys sys = WorkflowSys.builder()
                    .context(context)
                    .callback(callback)
                    .build();
            context.setSys(sys);

            if(context.isResume(node.getNodeId())) {
                node.resume(context, callback);
            } else {
                node.run(context, callback);
            }

            if(context.isSuspended()) {
                context.getState().setStatus(WorkflowRunStatus.suspended);
                callback.onWorkflowRunSuspended(context);
            } else {
                context.getState().setStatus(WorkflowRunStatus.succeeded);
                callback.onWorkflowRunSucceeded(context);
            }
        } catch (Exception e) {
            LOGGER.info("node run failed, e: {}", Throwables.getStackTraceAsString(e));
            // single node does not require processing, swallow the exception
        } finally {
            removeRunningContext(context);
        }
    }

    public void resume(WorkflowContext context, IWorkflowCallback callback, Map<String, String> nodeIds) {
        try {
            addRunningContext(context);
            context.getState().setStatus(WorkflowRunStatus.running);
            context.putResumeNodeMapping(nodeIds);
            context.getState().putNextNodes(nodeIds.keySet());
            callback.onWorkflowRunResumed(context);
            if(context.getTriggerFrom().equals("DEBUG_NODE")) {
                runNode(context, callback, nodeIds.keySet().iterator().next());
            } else {
                run0(context, callback);
            }
        } finally {
            removeRunningContext(context);
        }
    }

    @SuppressWarnings("rawtypes")
    private void run0(WorkflowContext context, IWorkflowCallback callback) {
        CompletableFuture all = null;
        try {
            WorkflowSys sys = WorkflowSys.builder()
                    .context(context)
                    .callback(callback)
                    .build();
            context.setSys(sys);

            final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());
            List<CompletableFuture> fs = new ArrayList<>();
            while (!context.isFinish()) {
                List<BaseNode> nodes = context.getNextNodes();
                if(!nodes.isEmpty()) {
                    for (BaseNode node : nodes) {
                        synchronized(context) {
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
                    }
                } else {
                    if(!exceptions.isEmpty()) {
                        break;
                    }

                    if(context.isInterrupted()) {
                        break;
                    }

                    LockSupport.parkNanos(1000000); // 1ms
                }
            }

            all = CompletableFuture.allOf(fs.toArray(new CompletableFuture[0]));
            all.get(context.getNodeTimeout(), TimeUnit.SECONDS);

            if(!exceptions.isEmpty()) {
                throw exceptions.get(0);
            }

            if(context.isSuspended()) {
                context.getState().setStatus(WorkflowRunStatus.suspended);
                callback.onWorkflowRunSuspended(context);
            } else if(context.isInterrupted()) {
                context.getState().setStatus(WorkflowRunStatus.stopped);
                callback.onWorkflowRunStopped(context);
            } else {
                context.getState().setStatus(WorkflowRunStatus.succeeded);
                callback.onWorkflowRunSucceeded(context);
            }
        } catch (TimeoutException te) {
            LOGGER.info(te.getMessage(), te);
            context.getState().setStatus(WorkflowRunStatus.failed);
            callback.onWorkflowRunFailed(context, "节点执行超时", te);
            if(all != null) {
                all.cancel(true);
            }
        } catch (Throwable e) {
            LOGGER.info(e.getMessage(), e);
            e = Utils.getRootCause(e);
            context.getState().setStatus(WorkflowRunStatus.failed);
            callback.onWorkflowRunFailed(context, e.toString(), e);
        }
    }

    private static void addRunningContext(WorkflowContext ctx) {
        runnningContexts.put(ctx.getRunId(), ctx);
    }

    private static void removeRunningContext(WorkflowContext ctx) {
        runnningContexts.remove(ctx.getRunId());
    }
}
