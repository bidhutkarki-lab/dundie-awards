package com.ninjaone.dundie_awards.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ninjaone.dundie_awards.exception.EmployeeNotFoundException;
import com.ninjaone.dundie_awards.exception.OrganizationNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(fieldErrors);
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<Void> handleEmployeeNotFound(EmployeeNotFoundException exception) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(OrganizationNotFoundException.class)
    public ResponseEntity<Void> handleOrganizationNotFound(OrganizationNotFoundException exception) {
        return ResponseEntity.badRequest().build();
    }
}
