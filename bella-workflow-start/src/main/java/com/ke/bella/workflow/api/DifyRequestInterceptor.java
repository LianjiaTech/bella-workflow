package com.ke.bella.workflow.api;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.ke.bella.workflow.BellaContext;

/**
 * Unified processing request from bella, transfer header info to operator
 * information of BellaContext
 */
@Component
public class DifyRequestInterceptor extends HandlerInterceptorAdapter {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader("X-BELLA-TENANT-ID");
        // Bella带的tenantId
        if(StringUtils.hasText(tenantId) && tenantId.equals("04633c4f-8638-43a3-a02e-af23c29f821f")) {
            String userId = request.getHeader("X-BELLA-OPERATOR-ID");
            String userName = request.getHeader("X-BELLA-OPERATOR-NAME");
            Assert.notNull(userId, "获取用户信息失败");
            Assert.notNull(userName, "获取用户信息失败");
            try {
                // fixme: url解码失败先吞掉异常
                userName = URLDecoder.decode(userName, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
            }
            BellaContext.setOperator(Operator.builder().userId(Long.valueOf(userId)).userName(userName).tenantId(tenantId).build());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BellaContext.clearAll();
    }
}
