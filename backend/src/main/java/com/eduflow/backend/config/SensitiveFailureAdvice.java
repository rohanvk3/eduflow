package com.eduflow.backend.config;

import com.eduflow.backend.dto.ApiError;
import com.eduflow.backend.service.AuditRecorder;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class SensitiveFailureAdvice implements ResponseBodyAdvice<Object> {
    final AuditRecorder audit;

    public SensitiveFailureAdvice(AuditRecorder audit) {
        this.audit = audit;
    }

    public boolean supports(MethodParameter p, Class<? extends HttpMessageConverter<?>> c) {
        return true;
    }

    public Object beforeBodyWrite(Object body, MethodParameter p, MediaType type, Class<? extends HttpMessageConverter<?>> c, ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ApiError && request.getMethod() != HttpMethod.GET && request.getURI().getPath().startsWith("/api/admin/"))
            audit.failed("ADMINISTRATION");
        return body;
    }
}
