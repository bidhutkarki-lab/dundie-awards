package com.ninjaone.dundie_awards.exception;

public class OrganizationHasEmployeesException extends RuntimeException {

    public OrganizationHasEmployeesException(Long id) {
        super("Organization still has employees: " + id);
    }
}
