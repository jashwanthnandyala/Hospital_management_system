package com.Project.Hospital_Management_System.common.api;

public class ApiResponse<T> {
    public boolean success;
    public String message;
    public T data;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(String msg, T data) {
        return new ApiResponse<>(true, msg, data);
    }

    public static ApiResponse<Void> ok(String msg) {
        return new ApiResponse<>(true, msg);
    }
}
