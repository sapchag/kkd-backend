package com.sber.kkd.api.dto;

import java.time.LocalDate;

public record RunRefDto(
        String id,
        LocalDate snapshotDate
) {
}