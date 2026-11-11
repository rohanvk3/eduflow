package com.eduflow.backend.dto;

import java.util.Map;

public record ApiError(int status, String message, Map<String, String> errors,java.time.Instant timestamp,String errorCode,String path,String requestId) {
    public ApiError(int status,String message,Map<String,String> errors){this(status,message,errors,java.time.Instant.now(),"HTTP_"+status,org.slf4j.MDC.get("requestPath"),org.slf4j.MDC.get("requestId"));}
    public ApiError(int status, String message) {
        this(status, message, Map.of());
    }
}
