package com.Project.Hospital_Management_System.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApi(ApiException e) {
        return ResponseEntity.status(map(e.getCode()))
                .body(Map.of(
                        "timestamp", OffsetDateTime.now(),
                        "error", e.getCode().name(),
                        "message", e.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        var msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", OffsetDateTime.now(),
                "error", ErrorCode.BAD_REQUEST.name(),
                "message", msg
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOthers(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "timestamp", OffsetDateTime.now(),
                        "error", "INTERNAL_ERROR",
                        "message", e.getMessage() != null ? e.getMessage() : "Unexpected error"
                ));
    }

    private HttpStatus map(ErrorCode code) {
        return switch (code) {
            case BAD_REQUEST  -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND    -> HttpStatus.NOT_FOUND;
            case CONFLICT     -> HttpStatus.CONFLICT;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN    -> HttpStatus.FORBIDDEN;
            default           -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
