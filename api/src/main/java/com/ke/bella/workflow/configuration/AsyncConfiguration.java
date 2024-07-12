package com.ke.bella.workflow.configuration;

import lombok.val;
import org.apache.skywalking.apm.toolkit.trace.CallableWrapper;
import org.apache.skywalking.apm.toolkit.trace.RunnableWrapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 *
 * @author keboot
 */
@Configuration
public class AsyncConfiguration extends AsyncConfigurerSupport {
	@Override
	public Executor getAsyncExecutor() {
		val executor = new ThreadPoolTaskExecutor() {
			@Override
			public Future<?> submit(Runnable task) {
				return super.submit(getRunnableWrapper(task));
			}

			@Override
			public <T> Future<T> submit(Callable<T> task) {
				return super.submit(getCallableWrapper(task));
			}

			@Override
			public ListenableFuture<?> submitListenable(Runnable task) {
				return super.submitListenable(getRunnableWrapper(task));
			}

			@Override
			public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
				return super.submitListenable(getCallableWrapper(task));
			}

			@Override
			public void execute(Runnable task) {
				super.execute(getRunnableWrapper(task));
			}

			@Override
			public void execute(Runnable task, long startTimeout) {
				super.execute(getRunnableWrapper(task), startTimeout);
			}


			private Runnable getRunnableWrapper(Runnable task) {
				if (task instanceof RunnableWrapper) {
					return task;
				}
				return RunnableWrapper.of(task);
			}

			public <T> Callable<T> getCallableWrapper(Callable<T> task) {
				if (task instanceof CallableWrapper) {
					return task;
				}
				return CallableWrapper.of(task);
			}

		};
		executor.setThreadNamePrefix("AsyncExecutor-");
		executor.setCorePoolSize(20);
		executor.setQueueCapacity(100);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
		// gracefully shutdown
		executor.setWaitForTasksToCompleteOnShutdown(true);
		// change it by your condition
		executor.setAwaitTerminationSeconds(5);
		executor.initialize();

		return executor;
	}
}
