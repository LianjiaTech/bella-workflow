package com.ke.bella.workflow.api;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ke.bella.workflow.api.WorkflowOps.TriggerFrom;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowCApiRun;
import com.ke.bella.workflow.api.WorkflowOps.WorkflowRun;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.db.tables.pojos.WorkflowAsApiDB;
import com.ke.bella.workflow.service.WorkflowService;

@RestController
@Order
public class WorkflowCustomApi {

    @Autowired
    WorkflowController delegate;

    @Autowired
    WorkflowService ws;

    @PostMapping("/capi/**")
    public Object run(HttpServletRequest req, @RequestBody WorkflowCApiRun op1) {
        String host = (String) req.getAttribute("Host");
        if(StringUtils.isEmpty(host)) {
            host = req.getHeader("Host");
        }
        String path = (String) req.getAttribute("path");
        if(!StringUtils.hasText(host) || !StringUtils.hasText(path)) {
            return BellaResponse.builder()
                    .code(404)
                    .message("不合法的请求路径")
                    .build();
        }

        WorkflowAsApiDB api = ws.getCustomApi(host, path);
        if(api == null) {
            return BellaResponse.builder()
                    .code(404)
                    .message("不合法的请求路径")
                    .build();
        }

        WorkflowRun op = WorkflowRun.builder()
                .tenantId(api.getTenantId())
                .workflowId(api.getWorkflowId())
                .version(api.getVersion().longValue() < 0L ? null : api.getVersion())
                .triggerFrom(TriggerFrom.CUSTOM_API.name())
                .responseMode(op1.getResponseMode())
                .inputs(op1.getInputs())
                .callbackUrl(op1.getCallbackUrl())
                .traceId(op1.getTraceId())
                .spanLev(op1.getSpanLev())
                .metadata(op1.getMetadata())
                .query(op1.getQuery())
                .files(op1.getFiles())
                .threadId(op1.getThreadId())
                .userId(op1.getUserId())
                .userName(op1.getUserName())
                .stateful(op1.isStateful())
                .build();
        BellaContext.setOperator(op);
        return delegate.run(op);
    }
}
