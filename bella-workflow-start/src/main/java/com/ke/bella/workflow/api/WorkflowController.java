package com.ke.bella.workflow.api;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ke.bella.workflow.TaskExecutor;
import com.ke.bella.workflow.api.WorkflowOps.ResponseMode;
import com.ke.bella.workflow.api.WorkflowOps.TenantCreate;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowCopy;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowList;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowNodeRun;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowOp;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRun;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRunInfo;
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

    public static final long MAX_TIMEOUT = 300000L;

    @Autowired
    WorkflowService ws;

    @PostMapping("/draft/info")
    public WorkflowDB draftInfo(@RequestBody WorkflowOp op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");

        return ws.getDraftWorkflow(op.workflowId);
    }

    @PostMapping("/info")
    public WorkflowDB info(@RequestBody WorkflowOp op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");

        return ws.getPublishedWorkflow(op.workflowId);
    }

    @PostMapping("/draft/sync")
    public void sync(@RequestBody WorkflowSync op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.graph, "graph不能为空");

        if(StringUtils.isEmpty(op.getWorkflowId())) {
            ws.newWorkflow(op);
        } else {
            ws.syncWorkflow(op);
        }
    }

    @PostMapping("/copy")
    public void copy(@RequestBody WorkflowCopy op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");
        Assert.notNull(op.version, "version不能为空");

        WorkflowDB wf = ws.getWorkflow(op.workflowId, op.version);
        WorkflowSync sync = WorkflowSync.builder()
                .graph(wf.getGraph())
                .title(wf.getTitle())
                .desc(wf.getDesc())
                .build();
        ws.newWorkflow(sync);
    }

    @PostMapping("/draft/publish")
    public void publish(@RequestBody WorkflowOp op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");

        ws.publish(op.workflowId);
    }

    @PostMapping("/draft/run")
    public Object runDraft(@RequestBody WorkflowRun op) {
        return run0(op, "draft");
    }

    @PostMapping("/run")
    public Object run(@RequestBody WorkflowRun op) {
        return run0(op, "published");
    }

    public Object run0(WorkflowRun op, String ver) {
        ResponseMode mode = ResponseMode.valueOf(op.responseMode);
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");
        Assert.notNull(op.inputs, "inputs不能为空");
        Assert.notNull(mode, "responseMode必须为[streaming, blocking, callback]之一");
        if(mode == ResponseMode.callback) {
            Assert.notNull(op.callbackUrl, "callbackUrl不能为空");
        }

        WorkflowDB wf = ver.equals("published") ? ws.getPublishedWorkflow(op.workflowId)
                : ws.getDraftWorkflow(op.workflowId);
        Assert.notNull(wf, String.format("没有找到对应的的工作流, %s(ver. %s)", op.workflowId, ver));

        WorkflowRunDB wr = ws.newWorkflowRun(wf, op.inputs, op.callbackUrl, op.responseMode);

        if(mode == ResponseMode.blocking) {
            WorkflowRunBlockingCallback callback = new WorkflowRunBlockingCallback(ws, MAX_TIMEOUT);
            TaskExecutor.submit(() -> ws.runWorkflow(wr, op.inputs, callback));
            return callback.getWorkflowRunResult();

        } else if(mode == ResponseMode.streaming) {
            // create SseEmitter with timeout 300s
            SseEmitter emitter = SseHelper.createSse(MAX_TIMEOUT, wr.getWorkflowRunId());
            TaskExecutor.submit(() -> ws.runWorkflow(wr, op.inputs, new WorkflowRunStreamingCallback(emitter, ws)));
            return emitter;
        } else {
            TaskExecutor.submit(() -> {
                WorkflowRunNotifyCallback callback = new WorkflowRunNotifyCallback(ws, op.callbackUrl);
                ws.runWorkflow(wr, op.inputs, callback);
            });
            return BellaResponse.builder().code(201).data(wr).build();
        }
    }

    @PostMapping("/draft/node/run")
    public Object runSingleNode(@RequestBody WorkflowNodeRun op) {
        ResponseMode mode = ResponseMode.valueOf(op.responseMode);
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");
        Assert.hasText(op.nodeId, "nodeId不能为空");
        Assert.notNull(op.inputs, "inputs不能为空");
        Assert.notNull(mode, "responseMode必须为[streaming, blocking]之一");

        WorkflowDB wf = ws.getDraftWorkflow(op.workflowId);
        Assert.notNull(wf, String.format("工作流当前无draft版本，无法单独调试节点", op.workflowId));

        WorkflowRunDB wr = ws.newWorkflowRun(wf, op.inputs, "", op.responseMode);
        if(mode == ResponseMode.blocking) {
            SingleNodeRunBlockingCallback callback = new SingleNodeRunBlockingCallback();
            ws.runNode(wr, op.nodeId, op.inputs, callback);
            return callback.getWorkflowNodeRunResult(MAX_TIMEOUT);

        } else {
            // create SseEmitter with timeout 300s
            SseEmitter emitter = SseHelper.createSse(MAX_TIMEOUT, wr.getWorkflowRunId());
            ws.runNode(wr, op.nodeId, op.inputs, new SingleNodeRunStreamingCallback(emitter));
            return emitter;
        }
    }

    @PostMapping("/tenant/create")
    public TenantDB createTenant(@RequestBody TenantCreate op) {
        Assert.hasText(op.tenantName, "tenantName不能为空");

        return ws.createTenant(op.tenantName);
    }

    @PostMapping("/run/list")
    public List<WorkflowRunDB> listWorkflowRun(@RequestBody WorkflowList op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");

        return ws.listWorkflowRun(op.workflowId, op.startTime);
    }

    @PostMapping("/run/info")
    public WorkflowRunDB getWorkflowRun(@RequestBody WorkflowRunInfo op) {
        Assert.hasText(op.workflowRunId, "workflowRunId不能为空");

        return ws.getWorkflowRun(op.getWorkflowRunId());
    }

    @SuppressWarnings("rawtypes")
    @PostMapping("/callback/{tenantId}/{workflowId}/{workflowRunId}/{nodeId}")
    public BellaResponse callback(@PathVariable String tenantId,
            @PathVariable String workflowId,
            @PathVariable String workflowRunId,
            @PathVariable String nodeId,
            @RequestBody Map inputs) {

        WorkflowRunDB wr = ws.getWorkflowRun(workflowRunId);
        Assert.notNull(wr, String.format("找不到对应的工作流运行实例", workflowRunId));

        ws.notifyWorkflowRun(wr, nodeId, inputs);

        boolean isCallback = wr.getResponseMode().equals(ResponseMode.callback.name());

        // resume简单实现版：
        // 非callback时，等待超时时间到了之后再去试一下是否需要resume
        // callback时，可立即去尝试是否可以resume
        TaskExecutor.schedule(() -> {
            WorkflowRunNotifyCallback callback = new WorkflowRunNotifyCallback(ws, wr.getCallbackUrl());
            ws.tryResumeWorkflow(wr, callback);
        }, isCallback ? 10L : MAX_TIMEOUT + 5000L);

        return BellaResponse.builder().code(201).data("OK").build();
    }
}
