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
import com.ke.bella.workflow.WorkflowSchema;
import com.ke.bella.workflow.api.WorkflowOps.ResponseMode;
import com.ke.bella.workflow.api.WorkflowOps.TenantCreate;
import com.ke.bella.workflow.api.WorkflowOps.TriggerFrom;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowAsApiPublish;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowCopy;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowNodeRun;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowOp;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRun;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRunCancel;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRunInfo;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRunPage;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRunResponse;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowSync;
import com.ke.bella.workflow.api.callbacks.SingleNodeRunBlockingCallback;
import com.ke.bella.workflow.api.callbacks.SingleNodeRunStreamingCallback;
import com.ke.bella.workflow.api.callbacks.WorkflowRunBlockingCallback;
import com.ke.bella.workflow.api.callbacks.WorkflowRunNotifyCallback;
import com.ke.bella.workflow.api.callbacks.WorkflowRunStreamingCallback;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.db.tables.pojos.TenantDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowAsApiDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowNodeRunDB;
import com.ke.bella.workflow.db.tables.pojos.WorkflowRunDB;
import com.ke.bella.workflow.service.WorkflowService;
import com.ke.bella.workflow.utils.DifyUtils;
import com.ke.bella.workflow.utils.JsonUtils;

@RestController
@RequestMapping("/v1")
public class WorkflowController {

    public static final long MAX_TIMEOUT = 300000L;

    @Autowired
    WorkflowService ws;

    @PostMapping("/workflow")
    public WorkflowDB createApp(@RequestBody DifyController.DifyApp app) {
        return createApp0(app);
    }

    public WorkflowDB createApp0(DifyController.DifyApp app) {
        WorkflowSchema schema = DifyUtils.getDefaultWorkflowSchema();
        WorkflowSync sync = WorkflowSync.builder()
                .title(app.getName())
                .desc(app.getDescription())
                .graph(JsonUtils.toJson(schema))
                .mode(app.getMode())
                .build();
        return ws.newWorkflow(sync);
    }

    @PostMapping("/workflow/draft/info")
    public WorkflowDB draftInfo(@RequestBody WorkflowOp op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");

        return ws.getDraftWorkflow(op.workflowId);
    }

    @PostMapping("/workflow/info")
    public WorkflowDB info(@RequestBody WorkflowOp op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");

        return ws.getPublishedWorkflow(op.workflowId, op.version);
    }

    @PostMapping("/workflow/draft/sync")
    public WorkflowDB sync(@RequestBody WorkflowSync op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.graph, "graph不能为空");

        if(StringUtils.isEmpty(op.getWorkflowId())) {
            return ws.newWorkflow(op);
        } else {
            return ws.syncWorkflow(op);
        }
    }

    @PostMapping("/workflow/copy")
    public WorkflowDB copy(@RequestBody WorkflowCopy op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");
        Assert.notNull(op.version, "version不能为空");

        WorkflowDB wf = ws.getWorkflow(op.workflowId, op.version);
        WorkflowSync sync = WorkflowSync.builder()
                .graph(wf.getGraph())
                .title(wf.getTitle())
                .mode(wf.getMode())
                .desc(wf.getDesc())
                .build();
        return ws.newWorkflow(sync);
    }

    @PostMapping("/workflow/draft/publish")
    public WorkflowDB publish(@RequestBody WorkflowOp op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");

        return ws.publish(op.workflowId);
    }

    @PostMapping("/workflow/custom/publish")
    public WorkflowAsApiDB publishAsApi(@RequestBody WorkflowAsApiPublish op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");
        Assert.hasText(op.host, "host不能为空");
        Assert.hasText(op.path, "path不能为空");

        return ws.publishAsApi(op);
    }

    @PostMapping("/workflow/draft/run")
    public Object runDraft(@RequestBody WorkflowRun op) {
        op.setTriggerFrom(TriggerFrom.DEBUG.name());
        return run0(op, "draft");
    }

    @PostMapping("/workflow/run")
    public Object run(@RequestBody WorkflowRun op) {
        TriggerFrom tf = TriggerFrom.valueOf(op.triggerFrom);
        Assert.notNull(tf, "triggerFrom必须为[API, SCHEDULE]之一");

        return run0(op, "published");
    }

    @PostMapping("/workflow/cancel")
    public Object cancel(@RequestBody WorkflowRunCancel op) {
        Assert.hasText(op.runId, "runId不能为空");

        ws.cancelWorkflowRun(op);
        return BellaResponse.builder().code(201).data("OK").build();
    }

    @PostMapping("/{tenantId}/workflow/{workflowId}/run")
    public Object run(@PathVariable String tenantId, @PathVariable String workflowId, @RequestBody WorkflowRun op) {
        TriggerFrom tf = TriggerFrom.valueOf(op.triggerFrom);
        Assert.notNull(tf, "triggerFrom必须为[API, SCHEDULE]之一");
        op.setTenantId(tenantId);
        op.setWorkflowId(workflowId);
        BellaContext.setOperator(getPureOper(op));
        return run0(op, "published");
    }

    private static Operator getPureOper(Operator oper) {
        return Operator.builder()
                .userId(oper.getUserId())
                .userName(oper.getUserName())
                .email(oper.getEmail())
                .tenantId(oper.getTenantId())
                .spaceCode(oper.getSpaceCode())
                .build();
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

        WorkflowDB wf = ver.equals("published") ? ws.getPublishedWorkflow(op.workflowId, op.getVersion())
                : ws.getDraftWorkflow(op.workflowId);
        Assert.notNull(wf, String.format("没有找到对应的的工作流, %s(ver. %s), tenant: %s", op.workflowId, ver, op.tenantId));

        WorkflowRunDB wr = ws.newWorkflowRun(wf, op);

        if(mode == ResponseMode.blocking) {
            WorkflowRunBlockingCallback callback = new WorkflowRunBlockingCallback(ws, MAX_TIMEOUT);
            TaskExecutor.submit(() -> ws.runWorkflow(wr, op, callback));
            Map<String, Object> result = callback.getWorkflowRunResult();

            return BellaResponse.builder()
                    .code(200)
                    .data(result)
                    .build();

        } else if(mode == ResponseMode.streaming) {
            // create SseEmitter with timeout 300s
            SseEmitter emitter = SseHelper.createSse(MAX_TIMEOUT, wr.getWorkflowRunId());
            TaskExecutor.submit(() -> ws.runWorkflow(wr, op, new WorkflowRunStreamingCallback(emitter, ws)));
            return emitter;
        } else {
            TaskExecutor.submit(() -> {
                WorkflowRunNotifyCallback callback = new WorkflowRunNotifyCallback(ws, op.callbackUrl);
                ws.runWorkflow(wr, op, callback);
            });
            return BellaResponse.builder().code(201).data(wr).build();
        }
    }

    @PostMapping("/workflow/draft/node/run")
    public Object runSingleNode(@RequestBody WorkflowNodeRun op) {
        ResponseMode mode = ResponseMode.valueOf(op.responseMode);
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");
        Assert.hasText(op.nodeId, "nodeId不能为空");
        Assert.notNull(op.inputs, "inputs不能为空");
        Assert.notNull(mode, "responseMode必须为[streaming, blocking]之一");

        WorkflowDB wf = ws.getDraftWorkflow(op.workflowId);
        Assert.notNull(wf, String.format("工作流当前无draft版本，无法单独调试节点", op.workflowId));

        WorkflowRun op2 = WorkflowRun.builder()
                .userId(op.getUserId())
                .userName(op.getUserName())
                .tenantId(op.getTenantId())
                .workflowId(op.getWorkflowId())
                .inputs(op.getInputs())
                .responseMode(op.getResponseMode())
                .triggerFrom(WorkflowOps.TriggerFrom.DEBUG.name())
                .build();
        WorkflowRunDB wr = ws.newWorkflowRun(wf, op2);
        if(mode == ResponseMode.blocking) {
            SingleNodeRunBlockingCallback callback = new SingleNodeRunBlockingCallback(ws, MAX_TIMEOUT);
            ws.runNode(wr, op.nodeId, op.inputs, callback);
            return callback.getWorkflowNodeRunResult();

        } else {
            // create SseEmitter with timeout 300s
            SseEmitter emitter = SseHelper.createSse(MAX_TIMEOUT, wr.getWorkflowRunId());
            ws.runNode(wr, op.nodeId, op.inputs, new SingleNodeRunStreamingCallback(emitter));
            return emitter;
        }
    }

    @PostMapping("/workflow/tenant/create")
    public TenantDB createTenant(@RequestBody TenantCreate op) {
        Assert.hasText(op.tenantName, "tenantName不能为空");

        return ws.createTenant(op.tenantName, op.parentTenantId, op.getOpenapiKey());
    }

    @PostMapping("/workflow/run/page")
    public Page<WorkflowRunDB> listWorkflowRun(@RequestBody WorkflowRunPage op) {
        Assert.hasText(op.tenantId, "tenantId不能为空");
        Assert.hasText(op.workflowId, "workflowId不能为空");

        return ws.listWorkflowRun(op);
    }

    @PostMapping("/workflow/run/info")
    public WorkflowRunResponse getWorkflowRun(@RequestBody WorkflowRunInfo op) {
        Assert.hasText(op.workflowRunId, "workflowRunId不能为空");

        WorkflowRunDB wr = ws.getWorkflowRun(op.getWorkflowRunId());
        return WorkflowRunResponse.fromWorkflowRunDB(wr);
    }

    @SuppressWarnings("rawtypes")
    @PostMapping("/workflow/callback/{tenantId}/{workflowId}/{workflowRunId}/{nodeId}/{nodeRunId}")
    public BellaResponse callback(@PathVariable String tenantId,
            @PathVariable String workflowId,
            @PathVariable String workflowRunId,
            @PathVariable String nodeId,
            @PathVariable String nodeRunId,
            @RequestBody Map inputs) {
        BellaContext.setOperator(Operator.builder()
                .tenantId(tenantId)
                .userId(0L)
                .userName("callback")
                .build());

        WorkflowRunDB wr = ws.getWorkflowRun(workflowRunId);
        Assert.notNull(wr, String.format("找不到对应的工作流运行实例", workflowRunId));
        ws.notifyWorkflowRun(wr, nodeId, nodeRunId, inputs);

        TaskExecutor.submit(() -> ws.notifyWorkflowRun(wr));
        return BellaResponse.builder().code(201).data("OK").build();
    }

    @RequestMapping("/workflow/{workflowId}/workflow-runs/{workflowRunId}/node-executions")
    public DifyController.DifyNodeExecution getWorkflowNodeRuns(@PathVariable String workflowId,
            @PathVariable String workflowRunId) {
        List<WorkflowNodeRunDB> nodeRuns = ws.getNodeRuns(workflowRunId);
        return DifyController.DifyNodeExecution.builder().data(DifyUtils.transfer(nodeRuns)).build();
    }
}
