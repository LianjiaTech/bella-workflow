package com.ke.bella.workflow.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.ke.bella.workflow.api.WorkflowOps.DomainAdd;
import com.ke.bella.workflow.api.model.DifyModelResponse;
import com.ke.bella.workflow.api.model.ModelInfoService;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.db.tables.pojos.DomainDB;
import com.ke.bella.workflow.service.DataSourceService;
import com.ke.bella.workflow.tool.ApiTool;
import com.ke.bella.workflow.tool.BellaToolService;
import com.ke.bella.workflow.tool.BellaToolService.ToolCollect;
import com.ke.bella.workflow.utils.OpenapiUtil;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("/console/api/workspaces")
public class DifyWorkspacesController {
    @Autowired
    ModelInfoService modelInfoService;

    @Autowired
    DifyController dc;

    @Autowired
    DataSourceService ds;

    @GetMapping("/domain/list")
    public List<DomainDB> listDomains(@RequestParam(required = false) String prefix) {
        dc.initContext();
        return ds.listDomains(prefix);
    }

    @PostMapping("/domain/add")
    public BellaResponse<?> addDomain(@RequestBody DomainAdd domainOp) {
        dc.initContext();
        DomainDB data = ds.addDomain(domainOp);
        return BellaResponse.builder()
                .data(data)
                .build();
    }

    @GetMapping("/current/models/model-types/{model_type}")
    public DifyModelResponse llmModel(@PathVariable("model_type") String modelType) {
        dc.initContext();
        String apikey = BellaContext.getApiKey();
        return DifyModelResponse.builder()
                .data(modelInfoService.fetchModels(modelType, apikey))
                .build();
    }

    @GetMapping("/current/model-providers/{provider}/models/parameter-rules")
    public DifyModelResponse llmModelParams(@PathVariable("provider") String provider,
            @RequestParam("model") String model,
            @RequestParam(value = "modelType", defaultValue = "llm") String modelType) {
        dc.initContext();
        String apikey = BellaContext.getApiKey();
        return DifyModelResponse.builder()
                .data(modelInfoService.fetchParameterRules(model, modelType, provider, apikey))
                .build();
    }

    @GetMapping("/current/datasource/{type}")
    public Object listDataSources(@PathVariable("type") String type) {
        dc.initContext();
        return ds.listDataSources(type);
    }

    @GetMapping("/current/tools/api")
    public List<DifyApiToolProvider> apiTools() {
        // currently scroll to search all bella api tools
        List<ToolCollect> allToolCollects = Lists.newArrayList();
        int pageNo = 1;
        int pageSize = 1000;
        int totalPages = 1;
        while (pageNo <= totalPages) {
            BellaToolService.BellaToolMarketPage<ToolCollect> page = BellaToolService.listToolCollects(pageNo, pageSize);
            if(CollectionUtils.isEmpty(page.getList())) {
                break;
            }
            allToolCollects.addAll(page.getList());
            totalPages = (int) Math.ceil((double) page.getTotal() / pageSize);
            pageNo++;
        }
        return allToolCollects.stream().map(this::transfer).collect(Collectors.toList());
    }

    private DifyApiToolProvider transfer(ToolCollect toolCollect) {
        List<ApiTool.ToolBundle> toolBundles = OpenapiUtil.parseOpenapiToToolBundle(toolCollect.getToolSchema());

        List<DifyApiToolProvider.Tool> validTools = new ArrayList<>();
        for (ApiTool.ToolBundle tool : toolBundles) {
            try {
                DifyApiToolProvider.Tool transferdTool = transfer(tool);
                validTools.add(transferdTool);
            } catch (IllegalArgumentException e) {
                // fixme: there are some invalid tools in market...
                LOGGER.info("invalid tool, ignore tool: {}, e: {}", tool.getOperationId(), Throwables.getStackTraceAsString(e));
            }
        }
        return DifyApiToolProvider.builder()
                .id(String.valueOf(toolCollect.getToolCollectId()))
                .author(toolCollect.getCreateUcid())
                .name(toolCollect.getToolCollectName())
                .description(DifyApiToolProvider.I18nObject.defaultI18nObject(toolCollect.getToolCollectDesc()))
                .label(DifyApiToolProvider.I18nObject.defaultI18nObject(toolCollect.getToolCollectName()))
                .type(DifyApiToolProvider.ToolProviderType.api)
                .tools(validTools)
                .build();
    }

    private DifyApiToolProvider.Tool transfer(ApiTool.ToolBundle tool) {
        List<DifyApiToolProvider.Parameter> parameters = Optional.ofNullable(tool.getParams())
                .orElse(Collections.emptyList())
                .stream()
                .map(this::transfer)
                .collect(Collectors.toList());

        return DifyApiToolProvider.Tool.builder()
                .name(tool.getOperationId())
                .label(DifyApiToolProvider.I18nObject.builder()
                        .zh_Hans(tool.getOperationId()).build())
                .description(DifyApiToolProvider.I18nObject.builder()
                        .zh_Hans(tool.getSummary()).build())
                .parameters(parameters)
                .labels(Lists.newArrayList(tool.getOperationId()))
                .build();
    }

    private DifyApiToolProvider.Parameter transfer(ApiTool.ToolBundle.ToolParameter parameter) {
        return DifyApiToolProvider.Parameter.builder()
                .name(parameter.getName())
                .label(DifyApiToolProvider.I18nObject.defaultI18nObject(parameter.getName()))
                .human_description(DifyApiToolProvider.I18nObject.defaultI18nObject(parameter.getDescription()))
                .llm_description(parameter.getDescription())
                .form(parameter.getForm())
                .type(parameter.getType() == null ? null : parameter.getType().getValue())
                .required(parameter.getRequired())
                .build();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @SuperBuilder(toBuilder = true)
    @Data
    @SuppressWarnings("rawtypes")
    public static class DifyApiToolProvider {
        private String id;
        private String author;
        private String name;
        private I18nObject description;
        @Builder.Default
        private Icon icon = new Icon();
        private I18nObject label;
        private ToolProviderType type;
        private Map masked_credentials;
        private Map original_credentials;
        private Boolean is_team_authorization;
        private TeamCredentials team_credentials;
        @Default
        private Boolean allow_delete = true;
        private List<Tool> tools;
        private List<String> labels;

        public enum ToolProviderType {
            api
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @SuperBuilder(toBuilder = true)
        public static class Icon {
            @Builder.Default
            private String content = "\ud83d\udd75\ufe0f";
            @Builder.Default
            private String background = "#FEF7C3";
        }

        @AllArgsConstructor
        @NoArgsConstructor
        @SuperBuilder(toBuilder = true)
        @Data
        public static class TeamCredentials {
            private String auth_type;
            private String api_key_header;
            private String api_key_value;
            private String api_key_header_prefix;
        }

        @AllArgsConstructor
        @NoArgsConstructor
        @SuperBuilder(toBuilder = true)
        @Data
        public static class Tool {
            private String author;
            private String name;
            private I18nObject label;
            private I18nObject description;
            private List<Parameter> parameters;
            private List<String> labels;
        }

        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        @Data
        public static class Parameter {
            private String name;
            private I18nObject label;
            private I18nObject human_description;
            private String type;
            private String form;
            private String llm_description;
            private boolean required;
            @JsonProperty("default")
            private Object _default;
            private Object min;
            private Object max;
            private ParameterOption options;

            @AllArgsConstructor
            @NoArgsConstructor
            @Builder
            @Data
            public static class ParameterOption {
                private String value;
                private I18nObject label;
            }
        }

        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class I18nObject {
            private String zh_Hans;

            public static I18nObject defaultI18nObject(String zh_Hans) {
                return I18nObject.builder()
                        .zh_Hans(zh_Hans)
                        .build();
            }

            public String getZh_Hans() {
                return zh_Hans;
            }

            public String getEn_US() {
                return zh_Hans;
            }

            public String getPt_BR() {
                return zh_Hans;
            }
        }
    }
}
