package com.ke.bella.workflow.service;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.ke.bella.workflow.db.BellaContext;
import org.apache.commons.lang3.RandomUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskExecutor {
    static ThreadFactory tf = new NamedThreadFactory("bella-worker-", true);
    static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, tf);

    public static void schedule(Runnable r, long delayMills) {
        executor.schedule(new Task(r), delayMills, TimeUnit.MILLISECONDS);
    }

    public static void scheduleAtFixedRate(Runnable r, int period) {
        int initialDelay = period + RandomUtils.nextInt(1, period);
        executor.scheduleAtFixedRate(r, initialDelay, period, TimeUnit.SECONDS);
    }

    public static void submit(Runnable r) {
        executor.submit(new Task(r));
    }

    public static class Task implements Runnable {
        Runnable r;
        Map<String, Object> context;

        public Task(Runnable r) {
            this.r = r;
            this.context = BellaContext.snapshot();
        }

        @Override
        public void run() {
            BellaContext.replace(context);
            try {
                r.run();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                BellaContext.clearAll();
            }
        }
    }

    public static class NamedThreadFactory implements ThreadFactory {
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
}
