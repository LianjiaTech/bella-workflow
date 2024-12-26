package com.ke.bella.workflow.api.interceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.auth.AuthenticationException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.ke.bella.openapi.BellaContext;
import com.ke.bella.openapi.Operator;
import com.ke.bella.openapi.apikey.ApikeyInfo;

@Component
public class BellaCustomInterceptor extends HandlerInterceptorAdapter {
    private static final String X_BELLA_TENANT_ID = "X-BELLA-TENANT-ID";
    private static final String X_BELLA_OPERATOR_ID = "X-BELLA-OPERATOR-ID";
    private static final String X_BELLA_OPERATOR_NAME = "X-BELLA-OPERATOR-NAME";
    private static final String X_BELLA_OPERATOR_SPACE = "X-BELLA-OPERATOR-SPACE";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String apiKey = request.getHeader(HttpHeaders.AUTHORIZATION);
        if(apiKey != null && apiKey.startsWith("Bearer ")) {
            apiKey = apiKey.substring(7);
        }
        String tenantId = BellaContext.getHeader(X_BELLA_TENANT_ID);
        String operatorId = BellaContext.getHeader(X_BELLA_OPERATOR_ID);
        String operatorName = "";
        if(!StringUtils.isEmpty(request.getHeader(X_BELLA_OPERATOR_NAME))) {
            operatorName = URLDecoder.decode(BellaContext.getHeader(X_BELLA_OPERATOR_NAME), StandardCharsets.UTF_8.name());
        }
        String operatorSpace = BellaContext.getHeader(X_BELLA_OPERATOR_SPACE);
        if(apiKey == null || tenantId == null || operatorId == null || operatorName == null) {
            throw new AuthenticationException("missing required headers");
        }

        BellaContext.setOperator(Operator.builder()
                .userId(Long.parseLong(operatorId))
                .userName(operatorName)
                .spaceCode(operatorSpace)
                .tenantId(tenantId).build());
        BellaContext.setApikey(ApikeyInfo.builder().apikey(apiKey).build());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BellaContext.clearAll();
    }
}
