package com.kdt.reservation.common.response;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.function.Function;

public class ApiResponse<T> extends ResponseEntity<ApiResponse.Body<T>> {

    public record Body<T>(int status, T data) {
    }

    private ApiResponse(HttpStatus status, T data) {
        super(new Body<>(status.value(), data), status);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(HttpStatus.OK, data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(HttpStatus.OK, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(HttpStatus.CREATED, data);
    }

    public static <T> ApiResponse<PageResponse<T>> page(Page<T> page) {
        return success(PageResponse.from(page));
    }

    public static <E, T> ApiResponse<PageResponse<T>> page(Page<E> page, Function<? super E, ? extends T> mapper) {
        return success(PageResponse.from(page, mapper));
    }
}
