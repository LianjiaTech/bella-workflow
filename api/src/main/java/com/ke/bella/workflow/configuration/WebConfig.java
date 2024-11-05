package com.ke.bella.workflow.configuration;

import com.ke.bella.workflow.api.interceptor.ApikeyInterceptor;
import com.ke.bella.workflow.api.interceptor.ConcurrentStartInterceptor;
import com.ke.bella.workflow.api.interceptor.DifyRequestInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private DifyRequestInterceptor difyRequestInterceptor;
    @Autowired
    private ApikeyInterceptor apikeyInterceptor;
    @Autowired
    private ConcurrentStartInterceptor concurrentStartInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apikeyInterceptor)
                .addPathPatterns("/v1/**")
                .addPathPatterns("/capi/**")
                .order(50);
        registry.addInterceptor(difyRequestInterceptor)
                .addPathPatterns("/console/api/**")
                .addPathPatterns("/v1/**")
                .order(200);
        registry.addInterceptor(concurrentStartInterceptor)
                .order(Ordered.LOWEST_PRECEDENCE);
    }

}
