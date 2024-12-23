package com.ke.bella.workflow.api.interceptor;

import com.ke.bella.openapi.BellaContext;
import com.ke.bella.openapi.apikey.ApikeyInfo;
import org.apache.http.auth.AuthenticationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.ke.bella.workflow.api.interceptor.ConcurrentStartInterceptor.ASYNC_REQUEST_MARKER;

@Component
public class ApikeyInterceptor extends HandlerInterceptorAdapter {
    @Value("${spring.profiles.active}")
    private String profile;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(Boolean.TRUE.equals(request.getAttribute(ASYNC_REQUEST_MARKER))) {
            return true;
        }
        ApikeyInfo apikeyInfo = BellaContext.getApikeyIgnoreNull();
        if(apikeyInfo != null || profile.contains("junit")) {
            return true;
        }
        throw new AuthenticationException("invalid api key");
    }
}
