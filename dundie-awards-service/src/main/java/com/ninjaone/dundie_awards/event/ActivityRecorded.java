package com.ninjaone.dundie_awards.event;

import java.time.LocalDateTime;

public record ActivityRecorded(LocalDateTime occurredAt, String event) {

    // stamped when the change happened rather than when the listener drains the queue,
    // otherwise the feed's newest-first ordering would reflect executor scheduling
    public static ActivityRecorded of(String event) {
        return new ActivityRecorded(LocalDateTime.now(), event);
    }
}
