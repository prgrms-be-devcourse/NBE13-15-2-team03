package com.kdt.reservation.review;

import org.springframework.data.domain.Sort;

public enum ReviewSort {
    LATEST(Sort.by(Sort.Direction.DESC, "created_at")),
    RATING_DESC(Sort.by(Sort.Direction.DESC, "rating").and(Sort.by(Sort.Direction.DESC, "created_at"))),
    RATING_ASC(Sort.by(Sort.Direction.ASC, "rating").and(Sort.by(Sort.Direction.DESC, "created_at")));

    private final Sort sort;

    ReviewSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }
}