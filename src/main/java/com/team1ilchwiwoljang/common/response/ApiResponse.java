package com.team1ilchwiwoljang.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

// NOTE: data가 null이면 JSON응답에서 제외합니다.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T> (
        boolean success,
        String message,
        T data
) {

    private static final String DEFAULT_SUCCESS_MESSAGE = "요청이 성공했습니다.";

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                true,
               DEFAULT_SUCCESS_MESSAGE,
                data
        );
    }

    public static <T> ApiResponse<T> of(T data) {
        return success(data);
    }
}
