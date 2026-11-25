package com.kdt.reservation.accommodation;

import com.kdt.reservation.common.response.PageResponse;

public record AccommodationReviewListResponse(
        double averageRating,
        long reviewCount,
        PageResponse<AccommodationReviewResponse> reviews
) {
    public static AccommodationReviewListResponse of(PageResponse<AccommodationReviewResponse> reviews, double averageRating) {
        long count = reviews.totalElements();
        return new AccommodationReviewListResponse(averageRating, count, reviews);
    }
}
