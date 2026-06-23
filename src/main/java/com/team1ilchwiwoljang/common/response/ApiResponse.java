package com.team1ilchwiwoljang.common.response;

import lombok.Getter;

@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final T data;

    private ApiResponse(T data) {
        this.success = true;
        this.data = data;
    }

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data);
    }
}
