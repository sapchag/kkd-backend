package com.sber.kkd.api.exception;

import java.util.Map;

/**
 * Тело ответа при ошибке в едином формате:
 * {"error": {"code": "...", "message": "..."}}
 */
public record ApiErrorBody(Map<String, Object> error) {

    public static ApiErrorBody of(String code, String message) {
        return new ApiErrorBody(Map.of("code", code, "message", message));
    }
}