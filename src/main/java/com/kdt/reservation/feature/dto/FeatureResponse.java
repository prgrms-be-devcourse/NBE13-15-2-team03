package com.kdt.reservation.feature.dto;

import com.kdt.reservation.common.Feature;
import com.kdt.reservation.common.FeatureCategory;
import com.kdt.reservation.common.FeatureTarget;

// 편의시설 목록 조회 응답의 항목 하나 (명세 6.4)
public record FeatureResponse(
        Long featureId,             // 등록·수정(7.2, 7.3, 8.1, 8.2)과 필터(6.1)에서 이 값을 쓴다
        String code,
        String name,
        FeatureCategory category,
        FeatureTarget target        // ACCOMMODATION 이면 숙소용, ROOM 이면 객실용
) {

    // 엔티티의 필드명(featureCategory, featureTarget)을 명세의 응답 이름(category, target)으로 맞춘다.
    public static FeatureResponse from(Feature feature) {
        return new FeatureResponse(
                feature.getId(),
                feature.getCode(),
                feature.getName(),
                feature.getFeatureCategory(),
                feature.getFeatureTarget()
        );
    }
}
