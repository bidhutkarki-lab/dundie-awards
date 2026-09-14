package com.ninjaone.dundie_awards.dto;

import com.ninjaone.dundie_awards.model.Employee;
import com.ninjaone.dundie_awards.model.Organization;
import lombok.Builder;

@Builder
public record EmployeeResponse(
        long id,
        String firstName,
        String lastName,
        Integer dundieAwards,
        Long organizationId,
        String organizationName) {

    public static EmployeeResponse from(Employee employee) {
        Organization organization = employee.getOrganization();
        return EmployeeResponse.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .dundieAwards(employee.getDundieAwards())
                .organizationId(organization == null ? null : organization.getId())
                .organizationName(organization == null ? null : organization.getName())
                .build();
    }
}
