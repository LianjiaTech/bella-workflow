package com.ke.bella.workflow.configuration;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Order(Integer.MIN_VALUE + 1)
public class CustomApiFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String host = request.getHeader("X-BELLA-WORKFLOW-HOST");
        request.setAttribute("Host", host);
        request.setAttribute("path", request.getRequestURI());
        if(StringUtils.hasText(host)) {
            request.getRequestDispatcher("/capi").forward(request, servletResponse);
            return;
        }
        chain.doFilter(servletRequest, servletResponse);
    }

}
