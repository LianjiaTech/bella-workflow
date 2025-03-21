package com.ke.bella.workflow.api.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ke.bella.openapi.BellaResponse;
import com.ke.bella.openapi.metadata.Model;
import com.ke.bella.openapi.protocol.completion.CompletionModelFeatures;
import com.ke.bella.openapi.protocol.completion.CompletionModelProperties;
import com.ke.bella.workflow.service.Configs;
import com.ke.bella.workflow.utils.HttpUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ModelInfoService {
    public Collection<ModelResponse> fetchModels(String modelType, String apikey) {
        ModelType type = ModelType.of(modelType);
        List<Model> openapiModels = listActiveModels(type.getEndpoints(), apikey);
        if(CollectionUtils.isEmpty(openapiModels)) {
            return Lists.newArrayList();
        }
        Map<String, ModelResponse> modelMap = new HashMap<>();
        for(Model openapiModel : openapiModels) {
            String supplier = openapiModel.getOwnerName();
            modelMap.computeIfAbsent(supplier, this::createProvider)
                    .getModels()
                    .add(transfer(openapiModel, type));

        }
        return modelMap.values();
    }

    public List<ParameterRule> fetchParameterRules(String model, String modelType, String provider, String apikey) {
        Model openapiModel = queryModel(model, provider, apikey);
        if(openapiModel == null) {
            return Lists.newArrayList();
        }
        ModelType type = ModelType.of(modelType);
        if(type == ModelType.LLM) {
            CompletionModelProperties property = openapiModel.toProperties(CompletionModelProperties.class);
            CompletionModelFeatures feature = openapiModel.toFeatures(CompletionModelFeatures.class);
            return ParameterRule.getRules(property == null ? 0 : property.getMax_output_context(),
                    feature != null && feature.isJson_format(),
                    feature != null && feature.isJson_schema());
        }
        //其他模型模型类型未实现
        return Lists.newArrayList();
    }

    private ModelResponse.ModelEntity transfer(Model model, ModelType type) {
        if(type == ModelType.LLM) {
            CompletionModelFeatures feature = model.toFeatures(CompletionModelFeatures.class);
            CompletionModelProperties property = model.toProperties(CompletionModelProperties.class);
            return ModelResponse.ModelEntity.builder()
                    .model(model.getModelName())
                    .label(I18nObject.builder()
                            .en_US(model.getModelName())
                            .zh_Hans(model.getModelName())
                            .build())
                    .modelType(type.getValue())
                    .status(ModelStatus.ACTIVE.getValue())
                    .loadBalancingEnabled(true)
                    .features(feature == null ? Lists.newArrayList() : ModelFeature.features(feature.isFunction_call(),
                            feature.isFunction_call(), feature.isReason_content(), feature.isVision(),
                            feature.isStream_function_call()))
                    .fetchFrom("predefined-model")
                    .deprecated(false)
                    .modelProperties(property == null ? ModelPropertyKey.chatProperty(8192)
                            : ModelPropertyKey.chatProperty(property.getMax_input_context()))
                    .build();
        } else {
            //其他模型模型类型未实现
            return ModelResponse.ModelEntity.builder()
                    .model(model.getModelName())
                    .label(I18nObject.builder()
                            .en_US(model.getModelName())
                            .zh_Hans(model.getModelName())
                            .build())
                    .modelType(type.getValue())
                    .status(ModelStatus.ACTIVE.getValue())
                    .loadBalancingEnabled(true)
                    .features(Lists.newArrayList())
                    .fetchFrom("predefined-model")
                    .deprecated(false)
                    .modelProperties(Maps.newHashMap())
                    .build();
        }
    }

    private ModelResponse createProvider(String provider) {
       I18nObject smallIcon = I18nObject.builder()
                .zh_Hans("http://0.0.0.0:5001/console/api/workspaces/current/model-providers/openai/icon_small/zh_Hans")
                .en_US("http://0.0.0.0:5001/console/api/workspaces/current/model-providers/openai/icon_small/zh_Hans")
                .build();
        I18nObject largerIcon = I18nObject.builder()
                .zh_Hans("http://0.0.0.0:5001/console/api/workspaces/current/model-providers/openai/icon_large/zh_Hans")
                .en_US("http://0.0.0.0:5001/console/api/workspaces/current/model-providers/openai/icon_large/zh_Hans")
                .build();
        return ModelResponse
                .builder()
                .provider(provider)
                .label(I18nObject.builder()
                        .en_US(provider)
                        .zh_Hans(provider)
                        .build())
                .status(ModelStatus.ACTIVE.getValue())
                .iconSmall(smallIcon)
                .iconLarge(largerIcon)
                .models(new ArrayList<>())
                .build();
    }

    private List<Model> listActiveModels(List<String> endpoints, String apikey) {
        String url = "/v1/meta/model/list";
        Map<String, String> headers = ImmutableMap.of(HttpHeaders.AUTHORIZATION, "Bearer " + apikey);
        Map<String, List<String>> query = ImmutableMap.of("endpoints", endpoints, "status", Lists.newArrayList("active"));
        return HttpUtils.getWithMultiQuery(headers, Configs.OPEN_API_HOST + url, query, new TypeReference<BellaResponse<List<Model>>>() {})
                .getData();

    }

    private Model queryModel (String model, String provider, String apikey) {
        String url = "/v1/meta/model/list";
        Map<String, String> headers = ImmutableMap.of(HttpHeaders.AUTHORIZATION, "Bearer " + apikey);
        Map<String, String> query = ImmutableMap.of("modelName", model, "ownerName", provider);
        List<Model> models = HttpUtils.get(headers, Configs.OPEN_API_HOST + url, query, new TypeReference<BellaResponse<List<Model>>>() {})
                .getData();
        if(CollectionUtils.isEmpty(models)) {
            return null;
        } else {
            return models.get(0);
        }
    }
}
