package com.ninjaone.dundie_awards.dto;

import jakarta.validation.constraints.NotBlank;

public record OrganizationRequest(@NotBlank String name) {
}
