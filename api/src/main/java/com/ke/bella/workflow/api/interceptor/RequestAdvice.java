package com.ke.bella.workflow.api.interceptor;

import java.lang.reflect.Type;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import com.ke.bella.workflow.api.Operator;
import com.ke.bella.workflow.db.BellaContext;
import com.ke.bella.workflow.utils.JsonUtils;

@RestControllerAdvice
public class RequestAdvice extends RequestBodyAdviceAdapter {

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> clazz = methodParameter.getContainingClass();
        return clazz.getName().startsWith("com.ke.bella.workflow.api.");
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        if(body instanceof Operator) {
            Operator oper = JsonUtils.fromJson(JsonUtils.toJson(body), Operator.class);
            Optional.ofNullable(BellaContext.getOperator()).ifPresent(oldOperator -> {
                oper.setUserId(oldOperator.getUserId());
                oper.setUserName(oldOperator.getUserName());
                if(StringUtils.isNotEmpty(oldOperator.getTenantId())) {
                    oper.setTenantId(oldOperator.getTenantId());
                }
                if(StringUtils.isNotEmpty(oldOperator.getSpaceCode())) {
                    oper.setSpaceCode(oldOperator.getSpaceCode());
                }
            });
            BellaContext.setOperator(oper);
        }

        return body;
    }

}
