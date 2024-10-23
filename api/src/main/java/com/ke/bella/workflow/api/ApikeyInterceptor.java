package com.ke.bella.workflow.api;

import static com.ke.bella.workflow.api.ConcurrentStartInterceptor.ASYNC_REQUEST_MARKER;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.ke.bella.workflow.db.BellaContext;

@Component
public class ApikeyInterceptor extends HandlerInterceptorAdapter {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(Boolean.TRUE.equals(request.getAttribute(ASYNC_REQUEST_MARKER))) {
            return true;
        }
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if(auth != null && auth.startsWith("Bearer ")) {
            BellaContext.setApiKey(auth.substring(7));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BellaContext.clearAll();
    }
}
