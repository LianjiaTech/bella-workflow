package com.ke.bella.workflow.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;

@Configuration
@ConditionalOnProperty(name = "apollo.enabled", havingValue = "true")
@EnableApolloConfig
public class ApolloConfig {
}
