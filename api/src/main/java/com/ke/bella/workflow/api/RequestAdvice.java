package com.ke.bella.workflow.api;

import java.lang.reflect.Type;
import java.util.Optional;

import com.ke.bella.workflow.db.BellaContext;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

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
        String auth = inputMessage.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if(auth != null && auth.startsWith("Bearer ")) {
            BellaContext.setApiKey(auth.substring(7));
        }

        if(body instanceof Operator) {
            Operator oper = (Operator) body;
            Optional.ofNullable(BellaContext.getOperator()).ifPresent(oldOperator -> {
                oper.setUserId(oldOperator.getUserId());
                oper.setUserName(oldOperator.getUserName());
                oper.setTenantId(oldOperator.getTenantId());
            });
            BellaContext.setOperator(oper);
        }

        return body;
    }

}
