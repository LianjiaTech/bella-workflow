package com.ke.bella.workflow.api.interceptor;

import static com.ke.bella.workflow.api.interceptor.ConcurrentStartInterceptor.ASYNC_REQUEST_MARKER;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ke.bella.openapi.login.context.ConsoleContext;
import com.ke.bella.workflow.api.Operator;
import org.apache.http.auth.AuthenticationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.ke.bella.workflow.db.BellaContext;

/**
 * Unified processing request from bella, transfer header info to operator
 * information of BellaContext
 */
@Component
public class DifyRequestInterceptor extends HandlerInterceptorAdapter {
    @Value("${spring.profiles.active}")
    private String profile;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws AuthenticationException {
        if(profile.contains("junit") || Boolean.TRUE.equals(request.getAttribute(ASYNC_REQUEST_MARKER))) {
            return true;
        }
        String tenantId = request.getHeader("X-BELLA-TENANT-ID");
        // Bella带的tenantId
        if(StringUtils.hasText(tenantId)) {
            String userId = request.getHeader("X-BELLA-OPERATOR-ID");
            String userName = request.getHeader("X-BELLA-OPERATOR-NAME");
            String spaceCode = request.getHeader("X-BELLA-OPERATOR-SPACE");
            Assert.notNull(userId, "获取用户信息失败");
            Assert.notNull(userName, "获取用户信息失败");
            try {
                // fixme: url解码失败先吞掉异常
                userName = URLDecoder.decode(userName, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
            }
            BellaContext.setOperator(Operator.builder()
                    .userId(Long.valueOf(userId))
                    .userName(userName)
                    .tenantId(tenantId)
                    .spaceCode(spaceCode)
                    .build());
        } else {
            com.ke.bella.openapi.Operator operator = ConsoleContext.getOperator();
            if(operator == null) {
                throw new AuthenticationException("认证失败");
            }
            BellaContext.setOperator(Operator.builder()
                    .userId(operator.getUserId())
                    .userName(operator.getUserName())
                    .email(operator.getEmail())
                    .build());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BellaContext.clearAll();
    }
}
