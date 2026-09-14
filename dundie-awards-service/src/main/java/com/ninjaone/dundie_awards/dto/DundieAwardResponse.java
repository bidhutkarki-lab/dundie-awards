package com.ninjaone.dundie_awards.dto;

import java.time.LocalDateTime;

import com.ninjaone.dundie_awards.model.DundieAward;
import com.ninjaone.dundie_awards.model.Employee;
import lombok.Builder;

@Builder
public record DundieAwardResponse(
        long id,
        long recipientId,
        String recipientName,
        long giverId,
        String giverName,
        long organizationId,
        String organizationName,
        LocalDateTime awardedAt) {

    public static DundieAwardResponse from(DundieAward award) {
        return DundieAwardResponse.builder()
                .id(award.getId())
                .recipientId(award.getRecipient().getId())
                .recipientName(fullName(award.getRecipient()))
                .giverId(award.getGiver().getId())
                .giverName(fullName(award.getGiver()))
                .organizationId(award.getOrganization().getId())
                .organizationName(award.getOrganization().getName())
                .awardedAt(award.getAwardedAt())
                .build();
    }

    private static String fullName(Employee employee) {
        return employee.getFirstName() + " " + employee.getLastName();
    }
}
