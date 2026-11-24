package com.kdt.reservation.common.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
    
    public static <E, T> PageResponse<T> from(Page<E> page, Function<? super E, ? extends T> mapper) {
        return from(page.map(mapper));
    }
}