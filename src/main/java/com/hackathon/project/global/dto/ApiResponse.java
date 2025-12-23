package com.hackathon.project.global.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiResponse<T> {

    private boolean success;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .build();
    }

    public static ApiResponse failure() {
        return ApiResponse.builder()
            .success(false)
            .data(null)
            .build();
    }

}
