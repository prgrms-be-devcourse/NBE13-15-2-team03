package com.kdt.reservation.accommodation;

import java.time.LocalDateTime;

public record AccommodationReviewResponse(
        long reviewId,
        int rating,
        String title,
        String body,
        String[] imageUrls,
        Author author,
        String roomName,
        LocalDateTime createdAt
) {
    public static record Author(long userId, String nickname) {
    }
}
