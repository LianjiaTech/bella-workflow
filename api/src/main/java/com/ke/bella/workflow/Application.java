package com.ke.bella.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;

/**
 * 服务启动类
 *
 * @author keboot
 */
@EnableApolloConfig
@SpringBootApplication
@ComponentScan(value = { "com.ke.bella.workflow" })
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
