package com.sber.kkd.api.dto;

import java.util.List;

/**
 * Ответ GET /api/kkd/summary — сводка по подразделению.
 */
public record SummaryDto(
        RunRefDto run,
        RunRefDto comparedTo,
        UnitRefDto unit,
        List<MetricDto> metrics,
        List<SeverityCountDto> issuesBySeverity,
        DeltaCountsDto delta
) {
}