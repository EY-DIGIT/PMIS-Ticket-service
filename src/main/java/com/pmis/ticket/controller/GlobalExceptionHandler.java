package com.pmis.ticket.controller;

import com.pmis.ticket.web.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.NoSuchElementException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Validation failures — bad category, missing title, etc. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return response(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    /** Ticket / SLA config / document not found */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
        log.warn("Not found: {}", ex.getMessage());
        return response(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    /** FK violations, unique constraint failures, null constraint, etc. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String msg = extractDbMessage(ex);
        log.error("Data integrity violation: {}", msg);
        return response(HttpStatus.CONFLICT, "Data Integrity Violation", msg);
    }

    /** File too large */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileSize(MaxUploadSizeExceededException ex) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "File Too Large",
                "Uploaded file exceeds the maximum allowed size.");
    }

    /** File storage errors */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        log.error("Runtime error: {}", ex.getMessage(), ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ex.getMessage());
    }

    /** Catch-all */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again.");
    }

    // -------------------------------------------------------------------------

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .status(status.value())
                        .error(error)
                        .message(message)
                        .timestamp(System.currentTimeMillis())
                        .build());
    }

    /** Pull the most useful part out of a nested DataIntegrityViolationException. */
    private String extractDbMessage(DataIntegrityViolationException ex) {
        Throwable root = ex;
        while (root.getCause() != null) root = root.getCause();
        String msg = root.getMessage();
        if (msg == null) return "Database constraint violation.";
        // Return just the first line (PostgreSQL error line) — strip the Detail: part
        return msg.lines().findFirst().orElse(msg);
    }
}
