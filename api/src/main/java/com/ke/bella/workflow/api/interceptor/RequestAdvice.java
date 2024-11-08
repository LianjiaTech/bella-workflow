package com.ke.bella.workflow.api.interceptor;

import com.ke.bella.workflow.api.Operator;
import com.ke.bella.workflow.db.BellaContext;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;
import java.util.Optional;

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
            Operator oper = (Operator) body;
            Optional.ofNullable(BellaContext.getOperator()).ifPresent(oldOperator -> {
                oper.setUserId(oldOperator.getUserId());
                oper.setUserName(oldOperator.getUserName());
                oper.setTenantId(oldOperator.getTenantId());
                oper.setSpaceCode(oldOperator.getSpaceCode());
            });
            BellaContext.setOperator(oper);
        }

        return body;
    }

}
