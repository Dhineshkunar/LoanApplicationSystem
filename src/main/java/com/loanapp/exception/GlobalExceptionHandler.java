package com.loanapp.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized exception handling for every controller in the application.
 *
 * WHY @RestControllerAdvice INSTEAD OF try/catch IN EVERY CONTROLLER METHOD:
 * - Removes duplicated error-formatting code from every endpoint.
 * - Guarantees a single, consistent error JSON shape across the whole API
 *   (see ErrorResponse) — critical for API consumers.
 * - Keeps controllers focused on the happy path; exceptional paths are
 *   handled in exactly one place, which is also where you'd add
 *   monitoring/alerting hooks in a real production system.
 *
 * INTERVIEW Q: "Difference between @ControllerAdvice and
 * @RestControllerAdvice?"
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody, so every
 * handler method's return value is written straight to the HTTP response
 * body as JSON, same as @RestController does for normal endpoints.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(), fe.getDefaultMessage()));

        log.warn("Validation failed on {}: {}", request.getRequestURI(), fieldErrors);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed for one or more fields")
                .path(request.getRequestURI())
                .validationErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Catches IllegalStateException AND its subclass
     * InvalidStatusTransitionException (no separate handler needed for the
     * subclass — Spring dispatches to the most specific applicable
     * handler, and since we don't have a more specific one registered,
     * this one applies to both).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            IllegalStateException ex, HttpServletRequest request) {

        log.warn("Conflict: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /**
     * Catch-all safety net. Never leaks stack traces or internal details
     * to the client — logs them server-side instead. This is the single
     * most important handler for production security: without it, an
     * unhandled exception can return a raw stack trace (information
     * disclosure) or a generic Whitelabel Error Page instead of your API's
     * JSON contract.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception at {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support.", request);
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String message, HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(body);
    }
}
