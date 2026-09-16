package com.ninjaone.dundie_awards.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    // derived from the dundie_awards rows and incremented with each award; see V6 to rebuild
    @Column(name = "award_count", nullable = false)
    private int awardCount;

    @ManyToOne
    private Organization organization;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public Employee(String firstName, String lastName, Organization organization) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.organization = organization;
    }
}
