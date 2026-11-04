package com.sber.kkd.api.dto;

import java.util.List;

/**
 * Ответ GET /api/meta — справочные данные для интерфейса.
 */
public record MetaDto(
        List<UnitOptionDto> units,
        List<RuleRefDto> rules
) {
}