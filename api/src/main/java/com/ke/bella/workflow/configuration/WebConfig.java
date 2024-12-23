package com.ke.bella.workflow.configuration;

import com.ke.bella.openapi.client.OpenapiClient;
import com.ke.bella.openapi.request.BellaRequestFilter;
import com.ke.bella.workflow.api.interceptor.ApikeyInterceptor;
import com.ke.bella.workflow.api.interceptor.BellaCustomInterceptor;
import com.ke.bella.workflow.api.interceptor.ConcurrentStartInterceptor;
import com.ke.bella.workflow.api.interceptor.DifyRequestInterceptor;
import com.ke.bella.workflow.service.Configs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private ApikeyInterceptor apikeyInterceptor;
    @Autowired
    private DifyRequestInterceptor difyRequestInterceptor;
    @Autowired
    private ConcurrentStartInterceptor concurrentStartInterceptor;
    @Autowired
    private BellaCustomInterceptor bellaCustomInterceptor;

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
                .excludePathPatterns("/v1/workflow/trigger/callback/**")
                .excludePathPatterns("/v1/workflow/callback/**")
                .excludePathPatterns("/v1/custom/bella/**")
                .order(50);
        registry.addInterceptor(difyRequestInterceptor)
                .addPathPatterns("/console/api/**")
                .addPathPatterns("/v1/**")
                .excludePathPatterns("/v1/workflow/callback/**", "/v1/workflow/trigger/callback/**")
                .excludePathPatterns("/v1/custom/bella/**")
                .order(200);
        registry.addInterceptor(bellaCustomInterceptor)
                .addPathPatterns("/v1/custom/bella/**")
                .order(200 + 1);
        registry.addInterceptor(concurrentStartInterceptor)
                .order(Ordered.LOWEST_PRECEDENCE);
    }

    @Bean
    public OpenapiClient openapiClient(Configs config) {
        return new OpenapiClient(Configs.OPEN_API_HOST);
    }

    @Bean
    public FilterRegistrationBean<BellaRequestFilter> bellaRequestFilter(OpenapiClient openapiClient) {
        FilterRegistrationBean<BellaRequestFilter> filterRegistrationBean = new FilterRegistrationBean<>();
        BellaRequestFilter bellaRequestFilter = new BellaRequestFilter(Configs.SERVICE_NAME,openapiClient);
        filterRegistrationBean.setFilter(bellaRequestFilter);
        return filterRegistrationBean;
    }
}
