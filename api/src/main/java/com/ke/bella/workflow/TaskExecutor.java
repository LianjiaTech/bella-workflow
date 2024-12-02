package com.ke.bella.workflow;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.RandomUtils;

import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.service.Configs;
import com.ke.bella.workflow.utils.Utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskExecutor {
    static ThreadFactory tf = new NamedThreadFactory("bella-worker-", true);
    static ScheduledExecutorService executor = Executors.newScheduledThreadPool(Configs.TASK_THREAD_NUMS, tf);

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

    public static <T> T invoke(Callable<T> task, long timeout, long maxMemoryBytes) throws Exception {
        timeout = Math.min(Math.max(timeout, 1), Configs.MAX_EXE_TIME) * 1000L; // ms

        FutureTask<T> futureTask = new FutureTask<>(new CallableTask<>(task));
        Thread thread = new Thread(futureTask);
        thread.setDaemon(true);
        thread.setName("bella-sandbox-" + Thread.currentThread().getId());
        thread.start();
        long tid = thread.getId();
        T result = null;
        try {
            if(Configs.isThreadAllocatedMemorySupported && maxMemoryBytes > 0) {
                long elapsed = 0;
                long initMem = Utils.getThreadAllocatedBytes(tid);
                while (elapsed < timeout) {
                    long mem = Utils.getThreadAllocatedBytes(tid) - initMem;
                    if(mem > maxMemoryBytes) {
                        String msg = String.format("内存使用超出限制, 当前累计使用内存：%,d, 限制：%,d (tid: %d)", mem, maxMemoryBytes, tid);
                        throw new IllegalArgumentException(msg);
                    }

                    // 运行超过10s的话采样 cpu 时间
                    if(elapsed > 10 * 1000) {
                        long cpuTime = Utils.getThreadCpuTimeMills(thread.getId());
                        long rate = cpuTime * 100 / elapsed;
                        if(rate > Configs.MAX_CPU_TIME_RATE) {
                            String msg = String.format("CPU使用率超出限制, 当前累计使用CPU率：%d%%, 限制：%d%% (tid: %d)", rate, Configs.MAX_CPU_TIME_RATE, tid);
                            throw new IllegalArgumentException(msg);
                        }
                    }

                    if(futureTask.isDone()) {
                        result = futureTask.get();
                        break;
                    } else {
                        TimeUnit.MILLISECONDS.sleep(1);
                        elapsed += 1;
                    }
                    if(elapsed >= timeout) {
                        throw new TimeoutException();
                    }
                }
            } else {
                result = futureTask.get(timeout, TimeUnit.MILLISECONDS);
            }
        } catch (TimeoutException e) {
            futureTask.cancel(true);
            throw e;
        } catch (Exception e) {
            throw e;
        } finally {
            thread.interrupt();
        }
        return result;
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

    public static class CallableTask<T> implements Callable<T> {
        Callable<T> r;
        Map<String, Object> context;

        public CallableTask(Callable<T> r) {
            this.r = r;
            this.context = BellaContext.snapshot();
        }

        @Override
        public T call() throws Exception {
            BellaContext.replace(context);
            try {
                return r.call();
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
