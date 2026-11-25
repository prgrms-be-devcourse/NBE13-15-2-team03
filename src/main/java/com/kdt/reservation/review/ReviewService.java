package com.kdt.reservation.review;

import com.kdt.reservation.accommodation.AccommodationReviewListResponse;
import com.kdt.reservation.accommodation.AccommodationReviewResponse;
import com.kdt.reservation.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public AccommodationReviewListResponse getAccommodationReviews(Long id, ReviewSort sort, Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.toSort());
        Page<Review> page = this.reviewRepository.findByAccommodationId(id, sorted);
        PageResponse<AccommodationReviewResponse> reviews = PageResponse.from(page, AccommodationReviewResponse::from);
        double averageRating = this.reviewRepository.findAverageRatingByAccommodationId(id);
        return AccommodationReviewListResponse.from(reviews, averageRating);
    }
}
