package com.ninjaone.dundie_awards.dto;

import com.ninjaone.dundie_awards.model.Employee;
import com.ninjaone.dundie_awards.model.Organization;

public record EmployeeResponse(
        long id,
        String firstName,
        String lastName,
        Integer dundieAwards,
        Long organizationId,
        String organizationName) {

    public static EmployeeResponse from(Employee employee) {
        Organization organization = employee.getOrganization();
        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getDundieAwards(),
                organization == null ? null : organization.getId(),
                organization == null ? null : organization.getName());
    }
}
