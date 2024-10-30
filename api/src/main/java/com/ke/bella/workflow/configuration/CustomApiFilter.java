package com.ke.bella.workflow.configuration;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ke.bella.workflow.service.DataSourceService;

@Component
@Order(Integer.MIN_VALUE + 1)
public class CustomApiFilter implements Filter {

    @Autowired
    DataSourceService ds;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String host = request.getHeader("X-BELLA-WORKFLOW-HOST");
        if(StringUtils.isEmpty(host)) {
            host = request.getHeader("Host");
        }

        request.setAttribute("Host", host);
        request.setAttribute("path", request.getRequestURI());
        if(ds.isCustomDomain(host)) {
            request.getRequestDispatcher("/capi").forward(request, servletResponse);
            return;
        }
        chain.doFilter(servletRequest, servletResponse);
    }

}
