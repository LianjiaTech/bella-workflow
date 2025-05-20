package com.ke.bella.workflow.auth;

import java.net.URL;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class BasicAuthenticator implements HttpAuthenticator {

    private static final String AUTH_TYPE = "basic";
    private static final String PREFIX = "Basic ";

    @Override
    public String getType() {
        return AUTH_TYPE;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    @Override
    public String generateAuthorization(String apiKey, String secret, String method, URL url, Map<String, Object> variablePool) {
        return apiKey;
    }
}
