package com.cinesearch.config;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, String>> handleWebClientError(WebClientResponseException e) {
        log.warn("External API error: {} {}", e.getStatusCode(), e.getMessage());
        return ResponseEntity.status(e.getStatusCode())
                .body(Map.of("error", "External API error", "status", String.valueOf(e.getStatusCode().value())));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("Validation error: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Validation error", "details", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        log.warn("Request validation error: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid request body"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(500)
                .body(Map.of("error", "Internal server error"));
    }
}
