package com.ninjaone.dundie_awards.dto;

public record LeaderboardEntry(
        long recipientId,
        String recipientName,
        String organizationName,
        long awardCount) {
}
