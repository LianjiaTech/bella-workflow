package com.ke.bella.workflow.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class ConcurrentStartInterceptor extends HandlerInterceptorAdapter {
    public static final String ASYNC_REQUEST_MARKER = "ASYNC_REQUEST_MARKER";

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        request.setAttribute(ASYNC_REQUEST_MARKER, Boolean.TRUE);
    }
}
