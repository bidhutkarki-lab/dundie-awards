package com.ninjaone.dundie_awards.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activities")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    @Column(name = "event")
    private String event;

    public Activity() {

    }

    public Activity(LocalDateTime localDateTime, String event) {
        super();
        this.occurredAt = localDateTime;
        this.event = event;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getEvent() {
        return event;
    }

}
