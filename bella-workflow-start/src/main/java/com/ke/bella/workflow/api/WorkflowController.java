package com.ke.bella.workflow.api;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ke.bella.workflow.IWorkflowCallback;
import com.ke.bella.workflow.WorkflowCallbackAdaptor;
import com.ke.bella.workflow.WorkflowContext;
import com.ke.bella.workflow.db.tables.pojos.TenantDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.service.WorkflowService;

import lombok.Getter;
import lombok.Setter;

@RestController
@RequestMapping("/v1/workflow")
public class WorkflowController {

    WorkflowService ws;

    @Getter
    @Setter
    static class WorkflowOp extends Operator {
        String workflowId;
    }

    @PostMapping("/info")
    public WorkflowDB info(@RequestBody WorkflowOp op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");

        return ws.getWorkflow(op.workflowId);
    }

    @Getter
    @Setter
    static class WorkflowSync extends Operator {
        String workflowId;
        String graph;
    }

    @PostMapping("/sync")
    public void sync(@RequestBody WorkflowSync op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.graph, "graph不能为空");

        if(StringUtils.isEmpty(op.getWorkflowId())) {
            ws.newWorkflow(op.graph);
        }

        ws.syncWorkflow(op.workflowId, op.graph);
    }

    @PostMapping("/publish")
    public void publish(@RequestBody WorkflowOp op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");

        WorkflowDB wf = ws.getWorkflow(op.workflowId);
        ws.validateWorkflow(wf);
        ws.publish(op.workflowId);
    }

    enum ResponseMode {
        streaming,
        blocking,
        callback;
    }

    @Getter
    @Setter
    @SuppressWarnings("rawtypes")
    static class WorkflowRun extends WorkflowOp {
        Map inputs = new HashMap();
        String responseMode = ResponseMode.streaming.name();
        String callbackUrl;
    }

    @PostMapping("/run")
    public Object run(@RequestBody WorkflowRun op) {
        ResponseMode mode = ResponseMode.valueOf(op.responseMode);
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");
        Assert.notNull(op.inputs, "inputs不能为空");
        Assert.notNull(mode, "responseMode必须为[streaming, blocking, callback]之一");
        if(mode == ResponseMode.callback) {
            Assert.notNull(op.callbackUrl, "callbackUrl不能为空");
        }

        WorkflowRunDB wr = ws.newWorkflowRun(op.workflowId, op.inputs, op.responseMode, op.callbackUrl);

        if(mode == ResponseMode.blocking) {
            BlockingWorkflowRunCallback callback = new BlockingWorkflowRunCallback();
            ws.runWorkflow(wr, op.inputs, callback);
            return callback.getWorkflowRunResult(300000l);

        } else if(mode == ResponseMode.streaming) {
            // create SseEmitter with timeout 300s
            SseEmitter emitter = SseHelper.createSse(300000l, wr.getWorkflowRunId());
            ws.runWorkflow(wr, op.inputs, new StreamingWorkflowCallback(emitter));
            return emitter;
        } else {
            // TODO
            return wr;
        }
    }

    @Getter
    @Setter
    @SuppressWarnings("rawtypes")
    static class WorkflowNodeRun extends WorkflowOp {
        Map inputs = new HashMap();
        String nodeId;
        String responseMode = ResponseMode.streaming.name();
    }

    @PostMapping("/node/run")
    public Object runNode(@RequestBody WorkflowNodeRun op) {
        ResponseMode mode = ResponseMode.valueOf(op.responseMode);
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");
        Assert.hasText(op.nodeId, "nodeId不能为空");
        Assert.notNull(op.inputs, "inputs不能为空");
        Assert.notNull(mode, "responseMode必须为[streaming, blocking]之一");

        return ws.runNode(op.workflowId, op.nodeId, op.inputs, op.responseMode);
    }

    @Getter
    @Setter
    static class TenantCreate extends Operator {
        String tenantName;
    }

    @PostMapping("/tenant/create")
    public TenantDB createTenant(@RequestBody TenantCreate op) {
        Assert.hasText(op.tenantName, "tenantName不能为空");

        return ws.createTenant(op.tenantName);
    }

    class BlockingWorkflowRunCallback extends WorkflowCallbackAdaptor {
        @Override
        public void onWorkflowRunSucceeded(WorkflowContext context) {
            // TODO
        }

        public Object getWorkflowRunResult(long timeout) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
            // TODO
        }
    }

    class StreamingWorkflowCallback implements IWorkflowCallback {
        final SseEmitter emitter;

        public StreamingWorkflowCallback(SseEmitter emitter) {
            this.emitter = emitter;
        }

        @Override
        public void onWorkflowRunStarted(WorkflowContext context) {
            // TODO
            SseHelper.sendEvent(emitter, "onWorkflowRunStarted", null);
        }

        @Override
        public void onWorkflowRunSucceeded(WorkflowContext context) {
            // TODO Auto-generated method stub
            SseHelper.sendEvent(emitter, "onWorkflowRunSucceeded", null);

        }

        @Override
        public void onWorkflowRunSuspended(WorkflowContext context) {
            // TODO Auto-generated method stub
            SseHelper.sendEvent(emitter, "onWorkflowRunSuspended", null);
        }

        @Override
        public void onWorkflowRunResumed(WorkflowContext context) {
            // TODO Auto-generated method stub
            SseHelper.sendEvent(emitter, "onWorkflowRunResumed", null);

        }

        @Override
        public void onWorkflowRunFailed(WorkflowContext context, String error, Throwable t) {
            // TODO Auto-generated method stub
            SseHelper.sendEvent(emitter, "onWorkflowRunFailed", null);
        }

        @Override
        public void onWorkflowNodeRunStarted(WorkflowContext context, String nodeId) {
            // TODO Auto-generated method stub
            SseHelper.sendEvent(emitter, "onWorkflowNodeRunStarted", null);

        }

        @Override
        public void onWorkflowNodeRunProgress(WorkflowContext context, String nodeId) {
            // TODO Auto-generated method stub
            SseHelper.sendEvent(emitter, "onWorkflowNodeRunProgress", null);

        }

        @Override
        public void onWorkflowNodeRunSucceeded(WorkflowContext context, String nodeId) {
            // TODO Auto-generated method stub
            SseHelper.sendEvent(emitter, "onWorkflowNodeRunSucceeded", null);
        }

        @Override
        public void onWorkflowNodeRunFailed(WorkflowContext context, String nodeId, String error, Throwable t) {
            // TODO Auto-generated method stub
            SseHelper.sendEvent(emitter, "onWorkflowNodeRunFailed", null);
        }
    }
}
