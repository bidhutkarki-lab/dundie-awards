package com.ninjaone.dundie_awards.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.ninjaone.dundie_awards.exception.EmployeeNotFoundException;
import com.ninjaone.dundie_awards.exception.InvalidOrganizationReferenceException;
import com.ninjaone.dundie_awards.exception.OrganizationHasEmployeesException;
import com.ninjaone.dundie_awards.exception.OrganizationNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        log.warn("Request validation failed: {}", fieldErrors);
        return ResponseEntity.badRequest().body(fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolations(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> parameterName(violation) + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("Returning 400: {}", message);
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private static String parameterName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        return path.substring(path.lastIndexOf('.') + 1);
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEmployeeNotFound(EmployeeNotFoundException exception) {
        log.warn("Returning 404: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(OrganizationNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleOrganizationNotFound(OrganizationNotFoundException exception) {
        log.warn("Returning 404: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(InvalidOrganizationReferenceException.class)
    public ResponseEntity<Map<String, String>> handleInvalidOrganizationReference(
            InvalidOrganizationReferenceException exception) {
        log.warn("Returning 400: {}", exception.getMessage());
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(OrganizationHasEmployeesException.class)
    public ResponseEntity<Map<String, String>> handleOrganizationHasEmployees(
            OrganizationHasEmployeesException exception) {
        log.warn("Returning 409: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
    }
}
