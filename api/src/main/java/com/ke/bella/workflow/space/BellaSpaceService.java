package com.ke.bella.workflow.space;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.utils.HttpUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Component
public class BellaSpaceService {
    @Value("${openapi.host:http://example.com}")
    private String openapiHost;
    private static final String USER_SPACES_ROLES = "/v1/space/member/role";

    public SpaceRole userSpaceRoles() {

        Map<String, String> header = ImmutableMap.of("Authorization", "Bearer " + BellaContext.getApiKey());

        Map<String, String> params = new HashMap<>();
        params.put("memberUid", String.valueOf(BellaContext.getOperator().getUserId()));
        BellaSpaceListResp<SpaceRole> resp = HttpUtils.get(header, openapiHost + USER_SPACES_ROLES,
                params,
                new TypeReference<BellaSpaceListResp<SpaceRole>>() {
                });
        if(Objects.isNull(resp) || 200 != resp.getCode()) {
            throw new IllegalStateException("Failed to get user space roles");
        }
        return resp.getData();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Data
    public static class BellaSpaceListResp<T> {
        private Integer code;
        private String message;
        private Long timestamp;
        private T data;
        private String stacktrace;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Data
    public static class SpaceRole {
        private String roleCode;
        private String spaceCode;
        private String spaceName;
    }
}
