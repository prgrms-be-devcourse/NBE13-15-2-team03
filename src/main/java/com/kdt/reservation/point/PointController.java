package com.kdt.reservation.point;

import com.kdt.reservation.common.response.ApiResponse;
import com.kdt.reservation.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {
    private final PointService pointService;

    @GetMapping("/me")
    public ApiResponse<PointResponse> getMyPoint(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(this.pointService.getMyPoint(userId));
    }

    @GetMapping("/me/history")
    public ApiResponse<PageResponse<PointHistoryResponse>> getMyPointHistory(
            @RequestParam(required = false) PointType type,
            Pageable pageable,
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.page(this.pointService.getMyPointHistory(userId, type, pageable));
    }
}
