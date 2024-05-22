package com.ke.bella.workflow.api;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ke.bella.workflow.TaskExecutor;
import com.ke.bella.workflow.api.WorkflowOps.ResponseMode;
import com.ke.bella.workflow.api.WorkflowOps.TenantCreate;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowNodeRun;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowOp;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRun;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowSync;
import com.ke.bella.workflow.api.callbacks.SingleNodeRunBlockingCallback;
import com.ke.bella.workflow.api.callbacks.SingleNodeRunStreamingCallback;
import com.ke.bella.workflow.api.callbacks.WorkflowRunBlockingCallback;
import com.ke.bella.workflow.api.callbacks.WorkflowRunNotifyCallback;
import com.ke.bella.workflow.api.callbacks.WorkflowRunStreamingCallback;
import com.ke.bella.workflow.db.tables.pojos.TenantDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.service.WorkflowService;

@RestController
@RequestMapping("/v1/workflow")
public class WorkflowController {

    WorkflowService ws;

    @PostMapping("/info")
    public WorkflowDB info(@RequestBody WorkflowOp op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");

        return ws.getWorkflow(op.workflowId);
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
            WorkflowRunBlockingCallback callback = new WorkflowRunBlockingCallback();
            ws.runWorkflow(wr, op.inputs, callback);
            return callback.getWorkflowRunResult(300000l);

        } else if(mode == ResponseMode.streaming) {
            // create SseEmitter with timeout 300s
            SseEmitter emitter = SseHelper.createSse(300000l, wr.getWorkflowRunId());
            ws.runWorkflow(wr, op.inputs, new WorkflowRunStreamingCallback(emitter));
            return emitter;
        } else {
            TaskExecutor.submit(op, () -> {
                WorkflowRunNotifyCallback callback = new WorkflowRunNotifyCallback(op.callbackUrl);
                ws.runWorkflow(wr, op.inputs, callback);
            });
            return wr;
        }
    }

    @PostMapping("/node/run")
    public Object runSingleNode(@RequestBody WorkflowNodeRun op) {
        ResponseMode mode = ResponseMode.valueOf(op.responseMode);
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");
        Assert.hasText(op.nodeId, "nodeId不能为空");
        Assert.notNull(op.inputs, "inputs不能为空");
        Assert.notNull(mode, "responseMode必须为[streaming, blocking]之一");

        WorkflowRunDB wr = ws.newWorkflowRun(op.workflowId, op.inputs, op.responseMode, "");
        if(mode == ResponseMode.blocking) {
            SingleNodeRunBlockingCallback callback = new SingleNodeRunBlockingCallback();
            ws.runNode(wr, op.nodeId, op.inputs, callback);
            return callback.getWorkflowNodeRunResult(op.nodeId, 300000l);

        } else {
            // create SseEmitter with timeout 300s
            SseEmitter emitter = SseHelper.createSse(300000l, wr.getWorkflowRunId());
            ws.runNode(wr, op.nodeId, op.inputs, new SingleNodeRunStreamingCallback(emitter));
            return emitter;
        }
    }

    @PostMapping("/tenant/create")
    public TenantDB createTenant(@RequestBody TenantCreate op) {
        Assert.hasText(op.tenantName, "tenantName不能为空");

        return ws.createTenant(op.tenantName);
    }
}
