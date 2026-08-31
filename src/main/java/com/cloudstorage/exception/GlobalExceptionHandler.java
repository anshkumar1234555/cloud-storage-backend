package com.cloudstorage.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        Map<String, Object> response = Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 400,
                "error", "Bad Request",
                "message", message
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex) {

        String message = ex.getMessage();

        HttpStatus status = HttpStatus.BAD_REQUEST;

        if (message != null) {

            if (message.contains("permission")
                    || message.contains("access")) {

                status = HttpStatus.FORBIDDEN;

            } else if (message.contains("not found")
                    || message.contains("File not found")
                    || message.contains("Folder not found")) {

                status = HttpStatus.NOT_FOUND;
            }
        }

        Map<String, Object> response = Map.of(
                "timestamp", LocalDateTime.now(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message != null
                        ? message
                        : "Something went wrong"
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}