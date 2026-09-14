package com.ninjaone.dundie_awards.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "dundie_awards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DundieAward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id", nullable = false, updatable = false)
    private Employee recipient;

    @ManyToOne(optional = false)
    @JoinColumn(name = "giver_id", nullable = false, updatable = false)
    private Employee giver;

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @Column(name = "awarded_at", nullable = false, updatable = false)
    private LocalDateTime awardedAt;

    public DundieAward(Employee recipient, Employee giver, Organization organization, LocalDateTime awardedAt) {
        this.recipient = recipient;
        this.giver = giver;
        this.organization = organization;
        this.awardedAt = awardedAt;
    }
}
