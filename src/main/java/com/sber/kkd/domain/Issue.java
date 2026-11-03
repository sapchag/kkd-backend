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

@Entity
@Table(name = "kkd_issue")
@Getter
@Setter
@NoArgsConstructor
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "rule_code", nullable = false)
    private String ruleCode;

    @Column(name = "unit_id")
    private Long unitId;

    @Column(name = "object_ref", nullable = false)
    private String objectRef;

    @Column(nullable = false)
    private String severity;

    @Column(name = "actual_value")
    private String actualValue;

    @Column(name = "expected_value")
    private String expectedValue;

    private String recommendation;
}