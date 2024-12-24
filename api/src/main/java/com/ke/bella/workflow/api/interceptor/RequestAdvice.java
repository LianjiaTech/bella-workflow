package com.ke.bella.workflow.api.interceptor;

import com.ke.bella.openapi.BellaContext;
import com.ke.bella.openapi.Operator;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;


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
            if(BellaContext.getOperatorIgnoreNull() == null) {
                BellaContext.setOperator(getPureOper((Operator) body));
            }
        }

        return body;
    }

    private static Operator getPureOper(Operator oper) {
        return Operator.builder()
                .userId(oper.getUserId())
                .userName(oper.getUserName())
                .email(oper.getEmail())
                .tenantId(oper.getTenantId())
                .spaceCode(oper.getSpaceCode())
                .build();
    }

}
