package com.kdt.reservation.review;

import com.kdt.reservation.accommodation.AccommodationReviewListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;

    public AccommodationReviewListResponse getAccommodationReviews(Long id, ReviewSort sort, Pageable pageable) {
        return null;
    }
}
