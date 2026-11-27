package com.kdt.reservation.point;

import com.kdt.reservation.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
