package com.ke.bella.workflow.api;

import java.lang.reflect.Type;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import com.ke.bella.workflow.BellaContext;

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
            BellaContext.setOperator(oper);
        }

        return body;
    }

}
