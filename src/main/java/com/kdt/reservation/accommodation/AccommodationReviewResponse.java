package com.kdt.reservation.accommodation;

import com.kdt.reservation.review.Review;
import com.kdt.reservation.review.ReviewImage;
import com.kdt.reservation.user.Users;

import java.time.LocalDateTime;
import java.util.List;

public record AccommodationReviewResponse(
        long reviewId,
        int rating,
        String title,
        String body,
        List<String> imageUrls,
        Author author,
        String roomName,
        LocalDateTime createdAt
) {
    public static record Author(long userId, String nickname) {
    }
    
    public static AccommodationReviewResponse from(Review review) {
        Users user = review.getUser();
        Author author = new Author(user.getId(), user.getNickname());
        List<String> images = review.getImages().stream().map(ReviewImage::getUrl).toList();
        return new AccommodationReviewResponse(
                review.getId(),
                review.getRating(),
                review.getTitle(),
                review.getBody(),
                images,
                author,
                review.getReservation().getRoom().getName(),
                review.getCreated_at()
        );
    }
}
