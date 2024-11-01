package com.ke.bella.workflow.api.interceptor;

import static com.ke.bella.workflow.api.interceptor.ConcurrentStartInterceptor.ASYNC_REQUEST_MARKER;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ke.bella.workflow.service.ApikeyService;
import org.apache.http.auth.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.ke.bella.workflow.db.BellaContext;

@Component
public class ApikeyInterceptor extends HandlerInterceptorAdapter {
    @Autowired
    private ApikeyService apikeyService;
    @Value("${spring.profiles.active}")
    private String profile;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(Boolean.TRUE.equals(request.getAttribute(ASYNC_REQUEST_MARKER))) {
            return true;
        }
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if(auth != null && auth.startsWith("Bearer ")) {
            String apikey = auth.substring(7);
            if(apikeyService.isValid(apikey)) {
                BellaContext.setApiKey(apikey);
                return true;
            }
        }
//        if(profile.contains("junit")) {
//            return true;
//        }
//        throw new AuthenticationException("invalid api key");
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BellaContext.clearAll();
    }
}
