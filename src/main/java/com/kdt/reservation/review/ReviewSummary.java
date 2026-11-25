package com.kdt.reservation.review;

// 숙소별 리뷰 수와 평균 평점
public record ReviewSummary(Long accommodationId, long reviewCount, double averageRating) {
    public ReviewSummary {
        averageRating = Math.round(averageRating * 10) / 10.0;
    }

    public static ReviewSummary empty(Long accommodationId) {
        return new ReviewSummary(accommodationId, 0, 0.0);
    }
}