package com.kdt.reservation.accommodation;

import com.kdt.reservation.common.Feature;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 숙소 - 편의시설(Feature) 연결 엔티티 (N:M 을 풀어 주는 테이블).
 * - 기본키: (accommodation_id, feature_id) 복합키
 * - 규칙: feature.target 이 ACCOMMODATION 인 항목만 연결 가능 (서비스에서 검증할 것)
 */
@Entity
// "이 feature 를 가진 숙소 목록"(필터) 조회용 역방향 인덱스
@Table(name = "accommodation_feature",
        indexes = @Index(name = "idx_feature_accommodation",
                columnList = "feature_id, accommodation_id"))
@Getter
@NoArgsConstructor
public class AccommodationFeature {

    // 복합 기본키 (숙소 id + feature id 묶음)
    @EmbeddedId
    private AccommodationFeatureId id;

    // @MapsId 값은 DB 컬럼명이 아니라 "키 클래스의 자바 필드명" 이다.
    // 이 연관관계의 FK 값이 곧 id.accommodation_id 가 된다.
    @MapsId("accommodation_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    // 이 연관관계의 FK 값이 곧 id.feature_id 가 된다.
    @MapsId("feature_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id", nullable = false)
    private Feature feature;

    // 저장된(id 가 있는) Accommodation, Feature 를 넘겨야 한다.
    // 주의: AccommodationFeatureId 생성자 인자 순서가 (feature_id, accommodation_id) 이다.
    public AccommodationFeature(Accommodation accommodation, Feature feature) {
        this.id = new AccommodationFeatureId(feature.getId(), accommodation.getId());
        this.accommodation = accommodation;
        this.feature = feature;
    }
}
