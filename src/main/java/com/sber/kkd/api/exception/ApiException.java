package com.sber.kkd.api.exception;

/**
 * Исключение, мапящееся на HTTP-ответ с единым форматом ошибок
 * {"error": {"code": "...", "message": "..."}}.
 */
public class ApiException extends RuntimeException {

    private final int status;
    private final String code;

    public ApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public static ApiException notFound(String message) {
        return new ApiException(404, "NOT_FOUND", message);
    }

    public static ApiException runFailed(String message) {
        return new ApiException(409, "RUN_FAILED", message);
    }

    public static ApiException badParam(String message) {
        return new ApiException(422, "INVALID_PARAM", message);
    }
}