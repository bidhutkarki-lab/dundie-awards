package com.ninjaone.dundie_awards.dto;

import java.time.LocalDateTime;

import com.ninjaone.dundie_awards.model.DundieAward;
import lombok.Builder;

@Builder
public record DundieAwardResponse(
        long id,
        long recipientId,
        long giverId,
        long organizationId,
        LocalDateTime awardedAt) {

    public static DundieAwardResponse from(DundieAward award) {
        return DundieAwardResponse.builder()
                .id(award.getId())
                .recipientId(award.getRecipient().getId())
                .giverId(award.getGiver().getId())
                .organizationId(award.getOrganization().getId())
                .awardedAt(award.getAwardedAt())
                .build();
    }
}
