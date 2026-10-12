package com.galileo.api.common.api;

import java.time.Instant;

public record ApiResponse<T>(
        Instant timestamp,
        String message,
        T data
) {
    public static <T> ApiResponse<T> of(String message, T data) {
        return new ApiResponse<>(Instant.now(), message, data);
    }
}
