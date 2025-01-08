package com.ke.bella.workflow.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.jooq.tools.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ke.bella.openapi.BellaContext;
import com.ke.bella.openapi.client.OpenapiClient;
import com.ke.bella.openapi.protocol.files.File;
import com.ke.bella.openapi.space.RoleWithSpace;
import com.ke.bella.openapi.utils.FileUtils;
import com.ke.bella.workflow.api.DifyController.DifyWorkflowRun;
import com.ke.bella.workflow.db.tables.pojos.WorkflowAsApiDB;
import com.ke.bella.workflow.service.WorkflowService;
import com.ke.bella.workflow.utils.OpenAiUtils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;

@RestController
@RequestMapping("/console/api")
@Slf4j
public class DifyConsoleController {

    @Autowired
    DifyController dc;

    @Autowired
    WorkflowService ws;

    @Value("#{${bella.workflow.files-upload-configs}}")
    private Map filesUploadConfigs;

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

    @GetMapping("/tags")
    public Object listTag() {
        return Collections.emptyList();
    }

    @GetMapping("/files/upload/configs")
    public Object fileConfig() {
        return filesUploadConfigs;
    }

    @PostMapping("/files/upload")
    public File uploadFile(MultipartFile file) {
        String contentType = file.getContentType();

        String purpose = "assistants";
        if(!StringUtils.isEmpty(contentType)) {
            MediaType mediaType = FileUtils.getMediaType(contentType);
            String type = FileUtils.getType(mediaType);
            if("image".equals(type)) {
                purpose = "vision";
            }
        }

        InputStream is = null;
        try {
            is = file.getInputStream();

            OpenapiClient client = OpenAiUtils.defaultOpenApiClient();
            return client.uploadFile(BellaContext.getApikey().getApikey(), purpose, is, file.getOriginalFilename());

        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw new IllegalStateException(e);
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    LOGGER.warn(e.getMessage());
                }
            }
        }
    }
}
