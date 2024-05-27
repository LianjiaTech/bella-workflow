package com.ke.bella.workflow;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class IDGenerator {
    private static final String yyMMddHHmmss = "yyMMddHHmmss";
    private static final int MAX_COUNT = 10000000;
    private static String instanceId;

    public static final IDGenerator WORKFLOW_RUN_GEN = new IDGenerator("RUN-");

    private final int serialLength;
    private final int serialMask;
    private final String prefix;
    private final String serialFormat;
    private final AtomicInteger serialCounter = new AtomicInteger(0);

    public IDGenerator(String prefix) {
        this(prefix, 6);
    }

    public IDGenerator(String prefix, int serialLength) {
        this.prefix = prefix;
        this.serialLength = serialLength;
        this.serialFormat = "%0" + this.serialLength + "d";
        this.serialMask = Integer.parseInt("1" + String.format(serialFormat, 0));
    }

    public String generate() {
        String now = new SimpleDateFormat(yyMMddHHmmss).format(new Date());
        String code = String.format("%s%s%s%s", this.prefix, now, instanceId, nextTick());
        return code;
    }

    private String nextTick() {
        int val = serialCounter.incrementAndGet();
        if(val >= MAX_COUNT) {
            synchronized(serialCounter) {
                val = serialCounter.get();
                if(val >= MAX_COUNT) {
                    serialCounter.set(0);
                }
            }
            val = serialCounter.incrementAndGet();

        }
        return String.format(this.serialFormat, val % this.serialMask);
    }

    public static void setInstanceId(Long id) {
        int idx = id.intValue();
        if(idx > 9999) {
            throw new IllegalStateException("超出当前所能够支持的最大实例数");
        }
        instanceId = String.format("%04d", idx);
    }

    public static String newWorkflowRunId() {
        return WORKFLOW_RUN_GEN.generate();
    }

    public static String newTenantId() {
        return "TENT-" + UUID.randomUUID().toString();
    }

    public static String newWorkflowId() {
        return "WKFL-" + UUID.randomUUID().toString();
    }

    public static String timeStrFromCode(String code) {
        int index = code.indexOf('-') + 1;
        String str = code.substring(index, index + 12);
        return str;
    }

    public static LocalDateTime timeFromCode(String code) {
        int index = code.indexOf('-') + 1;
        String str = code.substring(index, index + 12);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(yyMMddHHmmss);
        return LocalDateTime.parse(str, formatter);
    }
}
