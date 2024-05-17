package com.ke.bella.workflow;

import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.ke.metric.HawkPropertiesAutoConfig;
import com.lianjia.hawk.config.HawkAutoConfigure;
import com.lianjia.hawk.config.HawkServletFilterAutoConfigure;
import com.lianjia.hawk.config.HawkWatchAutoConfigure;
import com.lianjia.infrastructure.sentinel.autoconfig.SentinelWebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;

@SpringBootApplication(exclude = { FlywayAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class, HawkAutoConfigure.class,
	HawkWatchAutoConfigure.class, HawkServletFilterAutoConfigure.class,
	SentinelWebMvcAutoConfiguration.class, HawkAutoConfigure.class,
	HawkServletFilterAutoConfigure.class, HawkPropertiesAutoConfig.class }, scanBasePackages = { "com.ke.bella.workflow" })
@EnableApolloConfig
public class TestApplication {
}
