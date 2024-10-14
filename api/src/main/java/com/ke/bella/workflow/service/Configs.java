package com.ke.bella.workflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Configs {

    public static String OPEN_API_BASE;

    public static String API_BASE;

    public static long MAX_EXE_TIME = 600; // 600s

    public static boolean GROOVY_SANDBOX_ENABLE = false;

    public static boolean isThreadAllocatedMemorySupported = false;

    public static long MAX_EXE_MEMORY_ALLOC = 100 * 1024 * 1024; // 100MB

    @Value("${bella.apiBase}")
    public void setApiBase(String apiBase) {
        API_BASE = apiBase;
    }

    @Value("${bella.openApiBase}")
    public void setOpenApiBase(String openApiBase) {
        OPEN_API_BASE = openApiBase;
    }

    @Value("${bella.workflow.sandbox.groovy}")
    public void setGroovySandbox(boolean value) {
        GROOVY_SANDBOX_ENABLE = value;
    }
}
