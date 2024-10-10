package com.ke.bella.workflow.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelResponse {

    private String provider;
    private I18nObject label;
    @JsonProperty("icon_small")
    private I18nObject iconSmall;
    @JsonProperty("icon_large")
    private I18nObject iconLarge;
    private String status;
    private List<ModelEntity> models;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModelEntity  {
        private String model;
        private I18nObject label;
        @JsonProperty("model_type")
        private String modelType;
        private String status;
        @JsonProperty("load_balancing_enabled")
        private boolean loadBalancingEnabled;
        private List<String> features;
        @JsonProperty("fetch_from")
        private String fetchFrom;
        private boolean deprecated;
        @JsonProperty("model_properties")
        private Map<String, Object> modelProperties;
    }
}

