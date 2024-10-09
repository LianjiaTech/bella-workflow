package com.ke.bella.workflow;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.RandomUtils;

import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.service.Configs;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskExecutor {
    static ThreadFactory tf = new NamedThreadFactory("bella-worker-", true);
    static NamedThreadFactory tf2 = new NamedThreadFactory("bella-sandbox-", true, true);
    static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1000, tf);

    public static void schedule(Runnable r, long delayMills) {
        executor.schedule(new Task(r), delayMills, TimeUnit.MILLISECONDS);
    }

    public static void scheduleAtFixedRate(Runnable r, int period) {
        int initialDelay = period + RandomUtils.nextInt(1, period);
        executor.scheduleAtFixedRate(r, initialDelay, period, TimeUnit.SECONDS);
    }

    public static CompletableFuture<Void> submit(Runnable r) {
        return CompletableFuture.runAsync(new Task(r), executor);
    }

    public static <T> T invoke(Callable<T> task, long timeout) throws Exception {
        timeout = Math.min(Math.max(timeout, 1), Configs.MAX_EXE_TIME);
        ExecutorService es = Executors.newSingleThreadExecutor(tf2);
        try {
            Future<T> futrure = es.submit(task);
            try {
                return futrure.get(timeout, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                futrure.cancel(true);
                throw e;
            } catch (Exception e) {
                throw e;
            }

        } finally {
            es.shutdownNow();
            tf2.stop();
        }
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
                if(!(e instanceof RuntimeException)) {
                    e = new RuntimeException(e);
                }
                throw (RuntimeException) e;
            } finally {
                BellaContext.clearAll();
            }
        }
    }

    public static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final boolean isDaemon;
        private final boolean trackThreads;
        private final UncaughtExceptionHandler handler;
        private final List<Thread> threads = Collections.synchronizedList(new ArrayList<>());

        public NamedThreadFactory(String prefix, boolean isDaemon) {
            this(prefix, isDaemon, null, false);
        }

        public NamedThreadFactory(String prefix, boolean isDaemon, boolean trackThreads) {
            this(prefix, isDaemon, null, trackThreads);
        }

        public NamedThreadFactory(String prefix, boolean isDaemon, UncaughtExceptionHandler handler, boolean trackThreads) {
            this.prefix = prefix;
            this.isDaemon = isDaemon;
            this.handler = handler;
            this.trackThreads = trackThreads;
        }

        @Override
        public Thread newThread(Runnable r) {
            final Thread t = new Thread(r, String.format("%s%d", prefix, threadNumber.getAndIncrement()));
            t.setDaemon(isDaemon);
            if(this.handler != null) {
                t.setUncaughtExceptionHandler(handler);
            }
            if(trackThreads) {
                threads.add(t);
            }
            return t;
        }

        @SuppressWarnings("deprecation")
        public void stop() {
            for (Thread thread : threads) {
                try {
                    thread.stop();
                } catch (UnsupportedOperationException e) {
                    thread.interrupt();
                }
            }
        }
    }
}
