package com.ke.bella.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import com.ke.bella.openapi.login.config.EnableBellaLogin;

/**
 * 服务启动类
 *
 * @author keboot
 */
@SpringBootApplication
@ComponentScan(value = { "com.ke.bella.workflow" })
@EnableBellaLogin
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
