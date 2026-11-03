package com.sber.kkd.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "kkd_goal")
@Getter
@Setter
@NoArgsConstructor
public class Goal {

    public static final String STATUS_ACTIVE = "ACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @Column(name = "owner")
    private String owner;

    @Column(name = "parent_goal_id")
    private Long parentGoalId;

    @Column(nullable = false)
    private String status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }
}