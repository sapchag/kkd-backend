package com.sber.kkd.api.dto;

public record SeverityCountDto(
        String code,
        Long current,
        Long previous,
        Long delta
) {
}