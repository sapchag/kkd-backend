package com.sber.kkd.api.dto;

/**
 * Счётчики дельты: новые, сохраняющиеся, исправленные проблемы.
 */
public record DeltaCountsDto(
        long newIssues,
        long persisting,
        long resolved
) {
}