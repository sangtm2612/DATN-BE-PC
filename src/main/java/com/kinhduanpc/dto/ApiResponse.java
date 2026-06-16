package com.kinhduanpc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private ErrorDto error;
    private PageMeta pagination;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder().success(true).data(data).message(message).build();
    }

    public static <T> ApiResponse<T> success(T data, PageMeta pagination) {
        return ApiResponse.<T>builder().success(true).data(data).pagination(pagination).build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .error(new ErrorDto(code, message))
            .build();
    }

    @Data @AllArgsConstructor
    public static class ErrorDto {
        private String code;
        private String message;
    }

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class PageMeta {
        private int page;
        private int limit;
        private long total;
        private int totalPages;
    }
}
