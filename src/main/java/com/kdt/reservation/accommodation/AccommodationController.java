package com.kdt.reservation.accommodation;

import com.kdt.reservation.accommodation.dto.AccommodationDetailResponse;
import com.kdt.reservation.common.response.ApiResponse;
import com.kdt.reservation.review.ReviewService;
import com.kdt.reservation.review.ReviewSort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 숙소 API 의 입구. 주소(URL)와 자바 메서드를 연결한다.
 *
 * [@RestController 를 붙인 이유]
 * "이 클래스의 메서드가 돌려주는 값을 JSON 으로 바꿔서 응답하라"는 표시다.
 *
 * [@RequestMapping("/api/accommodations") 를 붙인 이유]
 * 이 클래스의 모든 API 주소 앞부분이 /api/accommodations 로 같다.
 * 공통 앞부분을 한 번만 적어 두면 메서드마다 반복해서 쓰지 않아도 된다.
 *
 * [Controller 에 규칙을 쓰지 않는 이유]
 * 입구는 "받고, 넘기고, 돌려주기"만 한다. 실제 규칙은 Service 에 둔다.
 * 그래야 같은 규칙을 다른 곳에서도 다시 쓸 수 있고, 테스트하기도 쉽다.
 */
@RestController
@RequestMapping("/api/accommodations")
@RequiredArgsConstructor
public class AccommodationController {

    // 실제 처리를 맡길 서비스. 스프링이 생성자를 통해 넣어 준다. (@RequiredArgsConstructor)
    private final AccommodationService accommodationService;

    // 리뷰 관련 처리를 맡길 서비스. 숙소 리뷰 목록 API(아래 getAccommodationReviews)에서 쓴다. (리뷰 담당 D 가 추가)
    private final ReviewService reviewService;

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

    /**
     * 숙소 리뷰 목록 조회. GET /api/accommodations/{id}/reviews (명세 12.2, 이슈 #20, 리뷰 담당 D 가 작성)
     *
     * [리뷰 API 인데 숙소 컨트롤러에 있는 이유]
     * 주소가 /api/accommodations 로 시작하기 때문이다. 주소의 앞부분이 같은 API 는 같은 컨트롤러에 모은다.
     * 실제 처리는 리뷰 쪽 서비스(ReviewService)에 맡긴다.
     */
    @GetMapping("/{id}/reviews")
    public ApiResponse<AccommodationReviewListResponse> getAccommodationReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "LATEST") ReviewSort sort,
            Pageable pageable
    ) {
        return ApiResponse.success(this.reviewService.getAccommodationReviews(id, sort, pageable));
    }
}
