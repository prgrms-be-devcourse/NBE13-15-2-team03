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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final AccommodationRepository accommodationRepository;

    @Transactional(readOnly = true)
    public AccommodationReviewListResponse getAccommodationReviews(Long accommodationId, ReviewSort sort, Pageable pageable) {
        if (!this.accommodationRepository.existsActiveById(accommodationId)) {
            throw new BusinessException(ErrorCode.ACCOMMODATION_NOT_FOUND);
        }

        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.toSort());
        Page<Review> page = this.reviewRepository.findByAccommodationId(accommodationId, sorted);
        PageResponse<AccommodationReviewResponse> reviews = PageResponse.from(page, AccommodationReviewResponse::from);
        Double average = this.reviewRepository.findAverageRatingByAccommodationId(accommodationId);
        double averageRating = average == null ? 0.0 : Math.round(average * 10) / 10.0;
        return AccommodationReviewListResponse.of(reviews, averageRating);
    }

    // 숙소 id 목록으로 숙소별 리뷰 수와 평균 평점을 조회한다
    @Transactional(readOnly = true)
    public Map<Long, ReviewSummary> getReviewSummaries(List<Long> accommodationIds) {
        if (accommodationIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ReviewSummary> summaries = new HashMap<>();
        this.reviewRepository.findSummariesByAccommodationIds(accommodationIds)
                .forEach(summary -> summaries.put(summary.accommodationId(), summary));
        accommodationIds.forEach(id -> summaries.putIfAbsent(id, ReviewSummary.empty(id)));
        return summaries;
    }

    // 숙소 하나의 리뷰 수와 평균 평점을 조회한다
    @Transactional(readOnly = true)
    public ReviewSummary getReviewSummary(Long accommodationId) {
        return getReviewSummaries(List.of(accommodationId)).get(accommodationId);
    }
}
