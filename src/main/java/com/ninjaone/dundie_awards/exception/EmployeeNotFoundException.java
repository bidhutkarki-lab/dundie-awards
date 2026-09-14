package com.ninjaone.dundie_awards.exception;

public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(Long id) {
        super("Employee not found: " + id);
    }
}
