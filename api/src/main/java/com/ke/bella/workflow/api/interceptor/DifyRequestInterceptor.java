package com.ke.bella.workflow.api.interceptor;

import com.ke.bella.openapi.BellaContext;
import com.ke.bella.openapi.Operator;
import com.ke.bella.openapi.apikey.ApikeyInfo;
import com.ke.bella.workflow.service.WorkflowService;
import org.apache.http.auth.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.ke.bella.workflow.api.interceptor.ConcurrentStartInterceptor.ASYNC_REQUEST_MARKER;

/**
 * Unified processing request from bella, transfer header info to operator
 * information of BellaContext
 */
@Component
public class DifyRequestInterceptor extends HandlerInterceptorAdapter {
    @Value("${spring.profiles.active}")
    private String profile;

    @Autowired
    WorkflowService ws;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws AuthenticationException {
        if(profile.contains("junit") || Boolean.TRUE.equals(request.getAttribute(ASYNC_REQUEST_MARKER))) {
            return true;
        }

        if(BellaContext.getApikeyIgnoreNull() != null) {
            return true;
        }

        Operator operator = BellaContext.getOperatorIgnoreNull();
        if(operator == null) {
            throw new AuthenticationException("认证失败");
        }

        String tenantId = BellaContext.getHeader("X-BELLA-TENANT-ID");
        String spaceCode = BellaContext.getHeader("X-BELLA-OPERATOR-SPACE");
        BellaContext.setOperator(Operator.builder()
                .userId(operator.getUserId())
                .userName(operator.getUserName())
                .email(operator.getEmail())
                .tenantId(tenantId)
                .spaceCode(spaceCode)
                .build());

        if(BellaContext.getApikeyIgnoreNull() == null) {
            BellaContext.setApikey(ApikeyInfo.builder().apikey(ws.getTenantApiKey(tenantId)).build());
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BellaContext.clearAll();
    }
}
