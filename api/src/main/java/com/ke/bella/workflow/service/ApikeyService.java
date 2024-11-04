package com.ke.bella.workflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.ke.bella.openapi.BellaResponse;
import com.ke.bella.openapi.apikey.ApikeyInfo;
import com.ke.bella.workflow.utils.HttpUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public class ApikeyService {
    @Value("${openapi.host}")
    private String openapiHost;
    private Cache<String, Boolean> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    public ApikeyInfo getApikeyInfo(String apikey) {
        if(StringUtils.isEmpty(apikey)) {
            return null;
        }
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + apikey);
        String url = "/v1/apikey/whoami";
        BellaResponse<ApikeyInfo> bellaResp = HttpUtils.get(headers, openapiHost + url, null, new TypeReference<BellaResponse<ApikeyInfo>>(){});
        return bellaResp == null ? null : bellaResp.getData();
    }

    public boolean isValid(String apikey) {
        try {
            return cache.get(apikey, () -> getApikeyInfo(apikey) != null);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}

