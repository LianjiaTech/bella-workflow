package com.ke.bella.workflow;

import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ke.bella.workflow.db.tables.pojos.WorkflowTemplateDB;
import com.ke.bella.workflow.utils.JsonUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class WorkflowTemplate {
    private Long id;
    private String tenantId;
    private String spaceCode;
    private String templateId;
    private String workflowId;
    private Long version;
    private String title;
    private String mode;
    private String desc;
    private Set<String> tags;
    private Integer status;
    private Long copies;
    private Long cuid;
    private String cuName;
    private LocalDateTime ctime;
    private Long muid;
    private String muName;
    private LocalDateTime mtime;

    public static WorkflowTemplate from(WorkflowTemplateDB db) {
        WorkflowTemplate result = new WorkflowTemplate();

        result.setId(db.getId());
        result.setTenantId(db.getTenantId());
        result.setSpaceCode(db.getSpaceCode());
        result.setTemplateId(db.getTemplateId());
        result.setWorkflowId(db.getWorkflowId());
        result.setVersion(db.getVersion());
        result.setTitle(db.getTitle());
        result.setMode(db.getMode());
        result.setDesc(db.getDesc());
        result.setStatus(db.getStatus());
        result.setCopies(db.getCopies());
        result.setCuid(db.getCuid());
        result.setCuName(db.getCuName());
        result.setCtime(db.getCtime());
        result.setMuid(db.getMuid());
        result.setMuName(db.getMuName());
        result.setMtime(db.getMtime());
        result.setTags(JsonUtils.fromJson(db.getTags(), new TypeReference<Set<String>>() {
        }));
        return result;
    }
}
