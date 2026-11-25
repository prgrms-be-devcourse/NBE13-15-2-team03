package com.kdt.reservation.accommodation;

import com.kdt.reservation.common.response.PageResponse;

public record AccommodationReviewListResponse(
        double averageRating,
        int reviewCount,
        PageResponse<AccommodationReviewResponse> reviews
) {
}
