package com.tradeagent.common;

public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorInfo error
) {

    public record ErrorInfo(String code, String message) {}

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, new ErrorInfo(errorCode.getCode(), errorCode.getMessage()));
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String detail) {
        return new ApiResponse<>(false, null, new ErrorInfo(errorCode.getCode(), detail));
    }
}
