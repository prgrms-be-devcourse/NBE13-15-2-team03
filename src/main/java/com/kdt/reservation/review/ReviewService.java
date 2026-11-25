package com.kdt.reservation.review;

import com.kdt.reservation.accommodation.AccommodationRepository;
import com.kdt.reservation.accommodation.AccommodationReviewListResponse;
import com.kdt.reservation.accommodation.AccommodationReviewResponse;
import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
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
    private final AccommodationRepository accommodationRepository;

    // TODO: 예외처리 + 리팩토링(네이밍 / DTO 위치 고민)
    @Transactional(readOnly = true)
    public AccommodationReviewListResponse getAccommodationReviews(Long id, ReviewSort sort, Pageable pageable) {
        if (!this.accommodationRepository.existsActiveById(id)) {
            throw new BusinessException(ErrorCode.ACCOMMODATION_NOT_FOUND);
        }

        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.toSort());
        Page<Review> page = this.reviewRepository.findByAccommodationId(id, sorted);
        PageResponse<AccommodationReviewResponse> reviews = PageResponse.from(page, AccommodationReviewResponse::from);
        Double average = this.reviewRepository.findAverageRatingByAccommodationId(id);
        double averageRating = average == null ? 0.0 : Math.round(average * 10) / 10.0;
        return AccommodationReviewListResponse.from(reviews, averageRating);
    }
}
