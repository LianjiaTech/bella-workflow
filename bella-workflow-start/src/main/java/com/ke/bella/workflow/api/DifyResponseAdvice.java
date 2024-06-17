package com.ke.bella.workflow.api;

import com.ke.bella.workflow.BellaContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;

@RestControllerAdvice
@Slf4j
public class DifyResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> clazz = returnType.getContainingClass();
        return clazz.getName().startsWith("com.ke.bella.workflow.api.DifyController");
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        try {
            response.getHeaders().add("Cache-Control", "no-cache");
            if(body instanceof DifyController.DifyResponse) {
                response.setStatusCode(HttpStatus.valueOf(((DifyController.DifyResponse) body).getStatus()));
                return body;
            }
            return body;
        } finally {
            BellaContext.clearAll();
        }
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public BellaResponse<?> exceptionHandler(Exception e) {
        int code = 500;
        String msg = e.getLocalizedMessage();
        if(e instanceof IllegalArgumentException
                || e instanceof DataIntegrityViolationException
                || e instanceof MethodArgumentNotValidException) {
            code = 400;
        }

        if(e instanceof DataIntegrityViolationException) {
            msg = "非法的数据";
        }

        if(code == 500) {
            LOGGER.warn(e.getMessage(), e);
        } else {
            LOGGER.info(e.getMessage());
        }

        BellaResponse<?> er = new BellaResponse<>();
        er.setCode(code);
        er.setTimestamp(System.currentTimeMillis());
        er.setMessage(msg);
        if(code == 500) {
            er.setStacktrace(stacktrace(e));
        }

        return er;
    }

    private static String stacktrace(Throwable e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
