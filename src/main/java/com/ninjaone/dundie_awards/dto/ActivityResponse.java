package com.ninjaone.dundie_awards.dto;

import java.time.LocalDateTime;

import com.ninjaone.dundie_awards.model.Activity;

public record ActivityResponse(long id, LocalDateTime occurredAt, String event) {

    public static ActivityResponse from(Activity activity) {
        return new ActivityResponse(activity.getId(), activity.getOccurredAt(), activity.getEvent());
    }
}
