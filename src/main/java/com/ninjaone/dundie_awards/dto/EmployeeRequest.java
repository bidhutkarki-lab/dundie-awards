package com.ninjaone.dundie_awards.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmployeeRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull Long organizationId) {
}
