package com.kdt.reservation.feature;

import com.kdt.reservation.common.FeatureTarget;
import com.kdt.reservation.common.response.ApiResponse;
import com.kdt.reservation.feature.dto.FeatureResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/features")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureService featureService;

    // 편의시설 목록 조회. GET /api/features (명세 6.4, 권한: 공개)
    // target 은 선택 값이다. ACCOMMODATION, ROOM 이 아닌 값이 오면 공통 예외 처리기가 400 INVALID_INPUT 으로 응답한다.
    @GetMapping
    public ApiResponse<List<FeatureResponse>> getFeatures(@RequestParam(required = false) FeatureTarget target) {
        return ApiResponse.success(featureService.getFeatures(target));
    }
}
