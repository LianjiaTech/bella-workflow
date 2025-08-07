package com.ke.bella.workflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Configs {
    public static String SERVICE_NAME = "workflow";

    public static boolean TOOL_API_ENABLED;

    public static boolean DATASET_API_ENABLED;

    public static String OPEN_API_HOST;

    public static String OPEN_API_BASE;

    public static String API_BASE;

    public static String BELLA_TOOL_API_BASE;

    public static String DATASET_API_BASE;

    public static String JOB_QUEUE_BASE;

    public static String SAND_BOX_API_BASE;

    public static long MAX_EXE_TIME = 600; // 600s

    public static boolean GROOVY_SANDBOX_ENABLE = true;

    public static boolean isThreadAllocatedMemorySupported = false;

    public static long MAX_EXE_MEMORY_ALLOC = 200 * 1024 * 1024; // 200MB

    public static long MAX_CPU_TIME_RATE = 85;

    public static Integer TASK_THREAD_NUMS = 1000;

    public static Integer SCHEDULE_TASK_THREAD_NUMS = 100;

    public static Integer BATCH_TASK_THREAD_NUMS = 100;

    public static int INTERRUPTED_INTERVAL_ROWS = 1000;

    public static long HTTP_CLIENT_READ_TIMEOUT_SECONDS;

    @Value("${bella.toolApiEnabled}")
    public void setToolApiEnabled(boolean toolApiEnabled) {
        TOOL_API_ENABLED = toolApiEnabled;
    }

    @Value("${bella.datasetApiEnabled}")
    public void setDatasetApiEnabled(boolean datasetApiEnabled) {
        DATASET_API_ENABLED = datasetApiEnabled;
    }

    @Value("${bella.openapiHost}")
    public void setOpenApiHost(String openApiHost) {
        OPEN_API_HOST = openApiHost;
    }

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

    @Value("${bella.datasetApiBase}")
    public void setDatasetApiBase(String datasetApiBase) {
        DATASET_API_BASE = datasetApiBase;
    }

    @Value("${bella.job-queue.url}")
    public void setJobQueueBase(String jobQueueBase) {
        JOB_QUEUE_BASE = jobQueueBase;
    }

    @Value("${bella.workflow.sandbox.groovy}")
    public void setGroovySandbox(boolean value) {
        GROOVY_SANDBOX_ENABLE = value;
    }

    @Value("${bella.codeSandboxApiBase}")
    public void setSandboxApiBase(String sandboxApiBase) {
        SAND_BOX_API_BASE = sandboxApiBase;
    }

    @Value("${bella.task.threadNums}")
    public void setTaskThreadNums(Integer taskThreadNums) {
        TASK_THREAD_NUMS = taskThreadNums;
    }

    @Value("${bella.maxExeMemoryAlloc}")
    public void setMaxExeMemoryAlloc(long maxExeMemoryAlloc) {
        MAX_EXE_MEMORY_ALLOC = maxExeMemoryAlloc;
    }

    @Value("${bella.workflow.sandbox.rdbInterruptedIntervalRows}")
    public void setMaxQueryRows(int rdbInterruptedIntervalRows) {
        INTERRUPTED_INTERVAL_ROWS = rdbInterruptedIntervalRows;
    }

    @Value("${bella.api.tool.read-timeout-seconds}")
    public void setHttpClientReadTimeoutSeconds(long httpClientReadTimeoutSeconds) {
        HTTP_CLIENT_READ_TIMEOUT_SECONDS = httpClientReadTimeoutSeconds;
    }
}
