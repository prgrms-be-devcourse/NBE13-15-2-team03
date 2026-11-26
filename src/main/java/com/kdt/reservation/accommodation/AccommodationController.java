package com.kdt.reservation.accommodation;

import com.kdt.reservation.accommodation.dto.AccommodationDetailResponse;
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

    /**
     * 숙소 상세 조회. GET /api/accommodations/{id} (명세 6.2, 권한: 공개)
     *
     * [@GetMapping("/{id}") 를 쓴 이유]
     * 조회는 HTTP 의 GET 방식으로 받는다. 위의 공통 앞부분과 합쳐져
     * 최종 주소는 /api/accommodations/{id} 가 된다. {id} 자리에는 숙소 번호가 온다. (예: /api/accommodations/1)
     *
     * [@PathVariable 을 쓴 이유]
     * 주소의 {id} 자리에 들어온 값을 아래 Long id 변수에 넣어 달라는 표시다.
     * 숫자가 아닌 값(예: /api/accommodations/abc)이 오면 공통 예외 처리기가 400 으로 응답한다.
     *
     * [반환 타입이 ApiResponse 인 이유]
     * 팀의 모든 성공 응답은 { "status": 200, "data": ... } 모양으로 통일한다. (공통 응답 포맷, PR #26)
     * ApiResponse.success(값) 을 쓰면 그 모양으로 감싸지고 HTTP 상태 코드도 200 으로 나간다.
     */
    @GetMapping("/{id}")
    public ApiResponse<AccommodationDetailResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.success(accommodationService.getDetail(id));
    }
}
