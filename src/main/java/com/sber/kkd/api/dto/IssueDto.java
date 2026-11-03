package com.sber.kkd.api.dto;

/**
 * Строка таблицы найденных проблем (GET /api/kkd/issues).
 */
public record IssueDto(
        Long id,
        String rule,
        String severity,
        String objectRef,
        String unitId,
        String actualValue,
        String expectedValue,
        String recommendation,
        String deltaStatus
) {
}