package com.eduflow.backend.exception;

import com.eduflow.backend.dto.ApiError;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({org.springframework.web.servlet.resource.NoResourceFoundException.class,org.springframework.web.servlet.NoHandlerFoundException.class})
    ResponseEntity<ApiError> missingRoute(){return ResponseEntity.status(404).body(new ApiError(404,"Resource not found"));}
    @ExceptionHandler({org.springframework.web.bind.MissingServletRequestParameterException.class,org.springframework.web.multipart.support.MissingServletRequestPartException.class})
    ResponseEntity<ApiError> missingParameter(){return ResponseEntity.badRequest().body(new ApiError(400,"A required request field or file is missing"));}
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> unsupportedMethod(){return ResponseEntity.status(405).body(new ApiError(405,"HTTP method is not supported for this endpoint"));}
    @ExceptionHandler(org.springframework.dao.InvalidDataAccessApiUsageException.class)
    ResponseEntity<ApiError> invalidDataQuery(){return ResponseEntity.badRequest().body(new ApiError(400,"Invalid request; review the selected filters"));}
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex){org.slf4j.LoggerFactory.getLogger(getClass()).error("Request {} failed: {}",org.slf4j.MDC.get("requestId"),ex.getClass().getSimpleName());return ResponseEntity.status(500).body(new ApiError(500,"An unexpected error occurred. Contact the institute with the request ID."));}
    @ExceptionHandler(AssignmentConflictException.class)
    ResponseEntity<ApiError> assignmentConflict(AssignmentConflictException exception) {
        return ResponseEntity.status(409).body(new ApiError(409,exception.getMessage()));
    }
    @ExceptionHandler({AssignmentUploadTooLargeException.class,org.springframework.web.multipart.MaxUploadSizeExceededException.class})
    ResponseEntity<ApiError> uploadTooLarge() {
        return ResponseEntity.status(413).body(new ApiError(413,"File or request exceeds the configured upload limit"));
    }
    @ExceptionHandler(AssignmentStorageException.class)
    ResponseEntity<ApiError> storageUnavailable() {
        return ResponseEntity.status(500).body(new ApiError(500,"File storage is unavailable; please retry"));
    }
    @ExceptionHandler(ExamConflictException.class)
    ResponseEntity<ApiError> examConflict(ExamConflictException exception) {
        return ResponseEntity.status(409).body(new ApiError(409,exception.getMessage()));
    }
    @ExceptionHandler(FinanceConflictException.class)
    ResponseEntity<ApiError> financeConflict(FinanceConflictException exception) {
        return ResponseEntity.status(409).body(new ApiError(409,exception.getMessage()));
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflictingRecord() {
        return ResponseEntity.status(409).body(new ApiError(409,"A conflicting record already exists; refresh before retrying"));
    }
    @ExceptionHandler(AttendanceConflictException.class)
    ResponseEntity<ApiError> attendanceConflict(AttendanceConflictException exception) {
        return ResponseEntity.status(409).body(new ApiError(409,exception.getMessage()));
    }
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiError> staleAttendance() {
        return ResponseEntity.status(409).body(new ApiError(409,"The record changed; reload and try again"));
    }
    @ExceptionHandler(AcademicConflictException.class)
    ResponseEntity<ApiError> academicConflict(AcademicConflictException exception) {
        return ResponseEntity.status(409).body(new ApiError(409, exception.getMessage()));
    }
    @ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
    ResponseEntity<ApiError> concurrentChange() {
        return ResponseEntity.status(409).body(new ApiError(409, "Concurrent change; please retry"));
    }
    @ExceptionHandler(TeacherConflictException.class)
    ResponseEntity<ApiError> teacherConflict(TeacherConflictException exception) {
        return ResponseEntity.status(409).body(new ApiError(409, exception.getMessage()));
    }
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(404).body(new ApiError(404, exception.getMessage()));
    }

    @ExceptionHandler(StudentConflictException.class)
    ResponseEntity<ApiError> studentConflict(StudentConflictException exception) {
        return ResponseEntity.status(409).body(new ApiError(409, exception.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> invalidParameter() {
        return ResponseEntity.badRequest().body(new ApiError(400, "Invalid request parameter"));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ApiError> duplicateEmail(DuplicateEmailException exception) {
        return ResponseEntity.status(409).body(new ApiError(409, exception.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> authenticationFailed() {
        return ResponseEntity.status(401).body(new ApiError(401, "Invalid email or password"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> forbidden() {
        return ResponseEntity.status(403).body(new ApiError(403, "Access denied"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validationFailed(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(400, "Validation failed", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadableBody() {
        return ResponseEntity.badRequest().body(new ApiError(400, "Invalid JSON request"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> invalidInput(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ApiError(400, exception.getMessage()));
    }
}
