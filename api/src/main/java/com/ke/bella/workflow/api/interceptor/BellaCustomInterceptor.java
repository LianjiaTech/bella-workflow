package com.ke.bella.workflow.api.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.auth.AuthenticationException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.ke.bella.workflow.api.Operator;
import com.ke.bella.workflow.db.BellaContext;

@Component
public class BellaCustomInterceptor extends HandlerInterceptorAdapter {
    private static final String X_BELLA_TENANT_ID = "X-BELLA-TENANT-ID";
    private static final String X_BELLA_OPERATOR_ID = "X-BELLA-OPERATOR-ID";
    private static final String X_BELLA_OPERATOR_NAME = "X-BELLA-OPERATOR-NAME";
    private static final String X_BELLA_OPERATOR_SPACE = "X-BELLA-OPERATOR-SPACE";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String apiKey = request.getHeader(HttpHeaders.AUTHORIZATION);
        String tenantId = request.getHeader(X_BELLA_TENANT_ID);
        String operatorId = request.getHeader(X_BELLA_OPERATOR_ID);
        String operatorName = request.getHeader(X_BELLA_OPERATOR_NAME);
        String operatorSpace = request.getHeader(X_BELLA_OPERATOR_SPACE);
        if(apiKey == null || tenantId == null || operatorId == null || operatorName == null) {
            throw new AuthenticationException("missing required headers");
        }

        BellaContext.setOperator(Operator.builder()
                .userId(Long.parseLong(operatorId))
                .userName(operatorName)
                .spaceCode(operatorSpace)
                .tenantId(tenantId).build());
        BellaContext.setApiKey(apiKey);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BellaContext.clearAll();
    }
}
