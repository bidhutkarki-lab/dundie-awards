package com.ninjaone.dundie_awards.exception;

public class InvalidEmployeeReferenceException extends RuntimeException {

    public InvalidEmployeeReferenceException(Long id) {
        super("Employee not found: " + id);
    }
}
