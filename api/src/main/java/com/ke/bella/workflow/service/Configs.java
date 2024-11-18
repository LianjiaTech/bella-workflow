package com.ke.bella.workflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Configs {

    public static String OPEN_API_BASE;

    public static String API_BASE;

    public static String BELLA_TOOL_API_BASE;

    public static String BORE_API_BASE;

    public static String SAND_BOX_API_BASE;

    public static long MAX_EXE_TIME = 600; // 600s

    public static boolean GROOVY_SANDBOX_ENABLE = false;

    public static boolean isThreadAllocatedMemorySupported = false;

    public static long MAX_EXE_MEMORY_ALLOC = 200 * 1024 * 1024; // 200MB

    public static long MAX_CPU_TIME_RATE = 85;

    @Value("${bella.apiBase}")
    public void setApiBase(String apiBase) {
        API_BASE = apiBase;
    }

    @Value("${bella.openApiBase}")
    public void setOpenApiBase(String openApiBase) {
        OPEN_API_BASE = openApiBase;
    }

    @Value("${bella.toolApiBase}")
    public void setBellaToolApiBase(String bellaToolApiBase) {
        BELLA_TOOL_API_BASE = bellaToolApiBase;
    }

    @Value("${bella.boreApiBase}")
    public void setBoreApiBase(String boreApiBase) {
        BORE_API_BASE = boreApiBase;
    }

    @Value("${bella.workflow.sandbox.groovy}")
    public void setGroovySandbox(boolean value) {
        GROOVY_SANDBOX_ENABLE = value;
    }

    @Value("${bella.codeSandboxApiBase}")
    public void setSandboxApiBase(String sandboxApiBase) {
        SAND_BOX_API_BASE = sandboxApiBase;
    }

    @Value("${bella.maxEexMemoryAlloc}")
    public void setMaxEexMemoryAlloc(long maxEexMemoryAlloc) {
        MAX_EXE_MEMORY_ALLOC = maxEexMemoryAlloc;
    }
}
