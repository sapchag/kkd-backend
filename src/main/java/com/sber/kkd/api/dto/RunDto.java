package com.sber.kkd.api.dto;

import java.time.LocalDate;

/**
 * Строка списка прогонов (GET /api/runs).
 */
public record RunDto(
        String id,
        LocalDate snapshotDate,
        String status,
        String comparedTo,
        long issueCount
) {
}