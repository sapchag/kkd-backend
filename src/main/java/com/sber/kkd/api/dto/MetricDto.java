package com.sber.kkd.api.dto;

/**
 * Базовая метрика ответа summary.
 */
public record MetricDto(
        String key,
        String title,
        String unit,
        Double value,
        Double previous,
        Double delta,
        String trend,
        Boolean higherIsBetter
) {
}