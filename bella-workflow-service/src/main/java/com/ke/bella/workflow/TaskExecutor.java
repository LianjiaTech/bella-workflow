package com.ke.bella.workflow;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.ke.bella.workflow.api.Operator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskExecutor {
    static ThreadFactory tf = new NamedThreadFactory("bella-worker-", true);
    static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, tf);

    public static void schedule(Operator oper, Runnable r, long delayMills) {
        executor.schedule(new Task(r, oper), delayMills, TimeUnit.MILLISECONDS);
    }

    public static void submit(Operator oper, Runnable r) {
        executor.submit(new Task(r, oper));
    }

    public static class Task implements Runnable {
        Runnable r;
        Operator oper;

        public Task(Runnable r, Operator oper) {
            this.r = r;
            this.oper = oper;
        }

        @Override
        public void run() {
            BellaContext.setOperator(oper);
            try {
                r.run();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                BellaContext.removeOperator();
            }
        }
    }
}

class NamedThreadFactory implements ThreadFactory {
    private final String prefix;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final boolean isDaemon;
    private final UncaughtExceptionHandler handler;

    public NamedThreadFactory(String prefix, boolean isDaemon) {
        this(prefix, isDaemon, null);
    }

    public NamedThreadFactory(String prefix, boolean isDaemon, UncaughtExceptionHandler handler) {
        this.prefix = prefix;
        this.isDaemon = isDaemon;
        this.handler = handler;
    }

    @Override
    public Thread newThread(Runnable r) {
        final Thread t = new Thread(r, String.format("%s%d", prefix, threadNumber.getAndIncrement()));
        t.setDaemon(isDaemon);
        if(this.handler != null) {
            t.setUncaughtExceptionHandler(handler);
        }
        return t;
    }
}
