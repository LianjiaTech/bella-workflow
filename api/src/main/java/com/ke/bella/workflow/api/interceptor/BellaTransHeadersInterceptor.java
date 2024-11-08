package com.ke.bella.workflow.api.interceptor;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.ke.bella.workflow.db.BellaContext;

@Component
public class BellaTransHeadersInterceptor implements HandlerInterceptor {

    private static final String BELLA_HEADER_PREFIX = "x-bella-";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();

            if(headerName.startsWith(BELLA_HEADER_PREFIX)) {
                String headerValue = request.getHeader(headerName);
                headers.put(headerName, headerValue);
            }
        }
        BellaContext.setTransHeaders(headers);
        return true;
    }
}
