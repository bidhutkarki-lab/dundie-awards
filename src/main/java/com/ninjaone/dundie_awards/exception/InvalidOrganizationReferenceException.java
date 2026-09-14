package com.ninjaone.dundie_awards.exception;

public class InvalidOrganizationReferenceException extends RuntimeException {

    public InvalidOrganizationReferenceException(Long id) {
        super("Organization not found: " + id);
    }
}
