package com.ke.bella.workflow.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;

import com.ke.bella.workflow.WorkflowNodeRunException;
import com.ke.bella.workflow.service.Configs;

public class Utils {
    public static Throwable getRootCause(Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause != rootCause.getCause() && !(rootCause instanceof WorkflowNodeRunException)) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    public static long getThreadAllocatedBytes(long threadId) {
        if(!Configs.isThreadAllocatedMemorySupported) {
            return -1;
        }

        long bytes = 0;
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        try {
            Method m1 = threadMxBean.getClass().getDeclaredMethod("getThreadAllocatedBytes", long.class);
            m1.setAccessible(true);
            bytes = (Long) m1.invoke(threadMxBean, threadId);
        } catch (Exception e) {
            // ignore
            bytes = -1;
        }

        return bytes;
    }

    public static long getThreadCpuTimeMills(long threadId) {
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        try {
            return threadMxBean.getThreadCpuTime(threadId) / 1000000L;
        } catch (Exception e) {
            return -1;
        }
    }
}
