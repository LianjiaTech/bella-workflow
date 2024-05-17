package com.ke.bella.workflow;

import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 服务启动类
 *
 * @author keboot
 */
@EnableApolloConfig
@SpringCloudApplication
@ComponentScan(value = { "com.ke.bella.workflow" })
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
