package com.ke.bella.workflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Configs {

    public static String OPEN_API_BASE;

    public static String API_BASE;

    @Value("${bella.apiBase}")
    public void setApiBase(String apiBase) {
        API_BASE = apiBase;
    }

    @Value("${bella.openApiBase}")
    public void setOpenApiBase(String openApiBase) {
        OPEN_API_BASE = openApiBase;
    }
}
