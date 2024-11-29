package com.ke.bella.workflow.api;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ke.bella.openapi.space.RoleWithSpace;
import com.ke.bella.workflow.api.DifyController.DifyWorkflowRun;
import com.ke.bella.workflow.db.tables.pojos.WorkflowAsApiDB;
import com.ke.bella.workflow.service.WorkflowService;

@RestController
@RequestMapping("/console/api")
public class DifyConsoleController {

    @Autowired
    DifyController dc;

    @Autowired
    WorkflowService ws;

    @PostMapping("/capi/**")
    public Object forward(HttpServletRequest req, @RequestBody DifyWorkflowRun op1) {
        String host = req.getHeader("Host");
        String path = req.getRequestURI().substring("/console/api".length());

        WorkflowAsApiDB capi = ws.getCustomApi(host, path);
        op1.setTenantId(capi.getTenantId());
        op1.setWorkflowId(capi.getWorkflowId());
        return dc.workflowRun(capi.getWorkflowId(), op1);
    }

    @GetMapping("/space/role")
    public RoleWithSpace getSpaceRole() {
        return dc.getSpaceRole();
    }
}
