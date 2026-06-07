package com.hotelos.reception.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global error handler. Catches validation failures and unexpected exceptions
 * so the caller never sees a stack trace. Stack traces stay in the server log;
 * the wire response is a small, predictable JSON shape. (Task 3.2 — Error
 * handling and Data exposure.)
 */
@ControllerAdvice
public class ErrorHandler {

    /**
     * Malformed JSON or unknown enum values (e.g. urgency = "NUCLEAR"). Jackson
     * throws before bean validation gets a chance, so this needs its own
     * handler — otherwise the generic Exception handler fires and the caller
     * sees INTERNAL_ERROR for what is really a client mistake. (BUG-02.)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> onMalformed(HttpMessageNotReadableException ex) {
        String cause = ex.getMostSpecificCause().getMessage();
        // Strip line/column noise — keep the meaningful prefix
        String tidy = cause == null ? "Request body is invalid" : cause.split("\\R")[0];
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "VALIDATION_FAILED",
                "message", tidy
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> onValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "VALIDATION_FAILED",
                "message", details
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> onUnexpected(Exception ex) {
        // Log the real cause server-side; do not leak it to the caller
        System.err.println("[reception] unhandled: " + ex.getClass().getName() + " — " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "INTERNAL_ERROR",
                "message", "Something went wrong on our side."
        ));
    }
}
