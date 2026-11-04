package com.sber.kkd.api.dto;

public record UnitOptionDto(
        Long id,
        String code,
        String name,
        Long parentId
) {
}