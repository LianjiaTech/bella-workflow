package com.ke.bella.workflow.tool;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.ke.bella.openapi.BellaContext;
import com.ke.bella.workflow.service.Configs;
import com.ke.bella.workflow.utils.HttpUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class BellaToolService {

    private static final String SUCCESS_ERROR_NO = "0";

    private static final String GET_TOOL_PATH = "/getToolsByToolName";

    private static final String GET_TOOL_COLLECT_PATH = "/v1/api/market/collection/searchByPage";

    /**
     * fixme :
     * 目前工具集市不区分namespace/provider，通过toolName唯一确定一个工具；且虽然是获取Tool，但返回是List...
     *
     * @return
     */
    public static List<BellaTool> getTool(String toolName) {
        if(!Configs.TOOL_API_ENABLED) {
            return Collections.emptyList();
        }
        ImmutableMap<String, String> param = ImmutableMap.of("toolName", toolName);
        return get(Configs.BELLA_TOOL_API_BASE + GET_TOOL_PATH, param, new TypeReference<BellaToolMarketResp<List<BellaTool>>>() {
        });
    }

    public static BellaToolMarketPage<ToolCollect> listToolCollects(Integer pageNo, Integer pageSize) {
        if(!Configs.TOOL_API_ENABLED) {
            return new BellaToolMarketPage<>(0, pageSize, pageNo, Collections.emptyList());
        }
        Map<String, String> params = new HashMap<>();
        params.put("pageNo", String.valueOf(pageNo));
        params.put("pageSize", String.valueOf(pageSize));
		// 枚举值3表示查询空间可见(public以及该空间下)的工具....
        params.put("toolType", "3");
        params.put("spaceCode", BellaContext.getOperator().getSpaceCode());

        String toolListUrl = Configs.BELLA_TOOL_API_BASE + GET_TOOL_COLLECT_PATH;
        return get(
                toolListUrl, params, new TypeReference<BellaToolMarketResp<BellaToolMarketPage<ToolCollect>>>() {
                });
    }

    private static <T> T get(String url, Map<String, String> params, TypeReference<BellaToolMarketResp<T>> typeReference) {
        BellaToolMarketResp<T> resp = HttpUtils.get(url, params, typeReference);
        if(!resp.getErrno().equals(SUCCESS_ERROR_NO)) {
            throw new IllegalStateException(resp.getErrmsg());
        }
        return resp.getData();
    }

    /**
     * fixme：此枚举用于协助解析工具集市的响应AuthData结构
     * 此处命名和Bella保持一致
     */
    @AllArgsConstructor
    @Getter
    @SuppressWarnings("all")
    public enum BellaToolCredentialsType {
        None(0, "", "none", "", "", ""),
        Custom(1, "", "custom", "key", "ak", ""),
        Basic(2, "Basic ", "basic", "key", "ak", ""),
        Bearer(3, "Bearer ", "bearer", "key", "ak", ""),
        KeIam(4, "", "ke-IAM", "", "ak", "sk");

        private Integer code;
        private String prefix;
        private String authType;
        private String key;
        private String apiKey;
        private String secret;

        public static BellaToolCredentialsType from(Integer authType) {
            return Arrays.stream(BellaToolCredentialsType.values()).filter(i -> i.getCode().equals(authType))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Credential not found. authType: " + authType));
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Data
    public static class BellaToolMarketResp<T> {
        private String errno;
        private String errmsg;
        private T data;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Data
    public static class BellaToolMarketPage<T> {
        private Integer total;
        private Integer pageSize;
        private Integer pageNo;
        private List<T> list;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Data
    public static class ToolCollect {
        private String toolCollectName;
        private String namespaceName;
        private String toolCollectDesc;
        private String createUcid;
        private Integer toolCollectId;
        private String category;
        private String toolSchema;
        private List<BellaTool> tools;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Data
    public static class BellaTool {
        private Long modifyUcid;
        private String toolName;
        private String namespaceName;
        private String category;
        private String toolDesc;
        private Map<String, Object> toolSchema;
        private Integer toolType;
        private Integer toolStatus;
        private Integer authType;
        private AuthData authData;
        private String toolCollectName;
        private Integer toolId;

        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        @Data
        public static class AuthData {
            private String addTo;
            private List<Param> params;

            @Data
            public static class Param {
                private String key;
                private String value;
            }
        }
    }
}
