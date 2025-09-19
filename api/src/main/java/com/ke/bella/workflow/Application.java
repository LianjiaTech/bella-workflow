package com.ke.bella.workflow;

import com.ke.bella.queue.worker.WorkerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import com.ke.bella.openapi.login.config.EnableBellaLogin;
import org.springframework.context.annotation.Import;

/**
 * 服务启动类
 *
 * @author keboot
 */
@SpringBootApplication
@ComponentScan(value = { "com.ke.bella.workflow" })
@EnableBellaLogin
@Import(WorkerConfiguration.class)
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
