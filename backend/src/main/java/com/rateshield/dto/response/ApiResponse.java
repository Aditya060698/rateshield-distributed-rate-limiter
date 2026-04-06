package com.rateshield.dto.response;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        ApiError error
) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> failure(String message, ApiError error) {
        return new ApiResponse<>(false, message, null, error);
    }
}
