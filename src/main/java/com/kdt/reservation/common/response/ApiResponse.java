package com.kdt.reservation.common.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public record ApiResponse<T>(int status, T data) {
    
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return of(HttpStatus.OK, data);
    }
    
    public static ResponseEntity<ApiResponse<Void>> ok() {
        return of(HttpStatus.OK, null);
    }
    
    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return of(HttpStatus.CREATED, data);
    }

    private static <T> ResponseEntity<ApiResponse<T>> of(HttpStatus status, T data) {
        return ResponseEntity.status(status).body(new ApiResponse<>(status.value(), data));
    }
}