package com.ke.bella.workflow.space;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.ke.bella.openapi.BellaResponse;
import com.ke.bella.openapi.space.RoleWithSpace;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.utils.HttpUtils;

@Component
public class BellaSpaceService {
    @Value("${openapi.host:http://example.com}")
    private String openapiHost;
    private static final String USER_SPACES_ROLES = "/v1/space/member/role";

    public RoleWithSpace userSpaceRoles() {

        Map<String, String> header = ImmutableMap.of("Authorization", "Bearer " + BellaContext.getApiKey());

        Map<String, String> params = new HashMap<>();
        params.put("memberUid", String.valueOf(BellaContext.getOperator().getUserId()));
        params.put("spaceCode", String.valueOf(BellaContext.getOperator().getSpaceCode()));
        BellaResponse<RoleWithSpace> resp = HttpUtils.get(header, openapiHost + USER_SPACES_ROLES,
                params,
                new TypeReference<BellaResponse<RoleWithSpace>>() {
                });
        if(Objects.isNull(resp) || 200 != resp.getCode()) {
            throw new IllegalStateException("Failed to get user space roles");
        }
        return resp.getData();
    }

    public List<RoleWithSpace> listSpace() {
        Map<String, String> header = ImmutableMap.of("Authorization", "Bearer " + BellaContext.getApiKey());
        Map<String, String> params = new HashMap<>();
        params.put("memberUid", String.valueOf(BellaContext.getOperator().getUserId()));

        BellaResponse<List<RoleWithSpace>> resp = HttpUtils.get(header, openapiHost + "/v1/space/role/list",
                params,
                new TypeReference<BellaResponse<List<RoleWithSpace>>>() {
                });
        if(Objects.isNull(resp) || 200 != resp.getCode()) {
            throw new IllegalStateException("Failed to get user space roles");
        }
        return resp.getData();
    }

}
