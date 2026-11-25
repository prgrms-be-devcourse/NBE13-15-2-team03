package com.kdt.reservation.accommodation;

import com.kdt.reservation.common.response.ApiResponse;
import com.kdt.reservation.review.ReviewService;
import com.kdt.reservation.review.ReviewSort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/accommodations")
@RequiredArgsConstructor
public class AccommodationController {
    private final AccommodationService accommodationService;
    private final ReviewService reviewService;

    @GetMapping("/{id}/reviews")
    public ApiResponse<AccommodationReviewListResponse> getAccommodationReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "LATEST") ReviewSort sort,
            Pageable pageable
    ) {
        return ApiResponse.success(this.reviewService.getAccommodationReviews(id, sort, pageable));
    }
}
