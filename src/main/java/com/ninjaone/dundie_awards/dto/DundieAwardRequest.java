package com.ninjaone.dundie_awards.dto;

import jakarta.validation.constraints.NotNull;

public record DundieAwardRequest(
        @NotNull Long recipientId,
        @NotNull Long giverId) {
}
