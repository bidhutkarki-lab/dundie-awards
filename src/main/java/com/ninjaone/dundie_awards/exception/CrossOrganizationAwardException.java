package com.ninjaone.dundie_awards.exception;

public class CrossOrganizationAwardException extends RuntimeException {

    public CrossOrganizationAwardException(Long recipientId, Long giverId) {
        super("Dundie awards can only be given within the same organization: recipientId="
                + recipientId + " giverId=" + giverId);
    }
}
