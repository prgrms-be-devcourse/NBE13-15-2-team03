package com.kdt.reservation.accommodation;

import com.kdt.reservation.common.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 숙소-편의시설 연결(accommodation_feature) 테이블에서 데이터를 꺼내고 저장하는 창구.
 *
 * [연결 테이블이 따로 있는 이유]
 * 숙소 하나는 편의시설을 여러 개 가질 수 있고, 편의시설 하나(예: 주차장)도 여러 숙소에 쓰인다.
 * 이런 "여러 개 대 여러 개" 관계는 "어느 숙소가 어느 편의시설을 가졌는지"를 적어 두는 연결 테이블로 푼다.
 *
 * [JpaRepository<AccommodationFeature, AccommodationFeatureId> 의 뜻]
 * 이 엔티티의 기본키는 숫자 하나가 아니라 (숙소 번호, 편의시설 번호) 두 값의 묶음이다.
 * 그 묶음을 표현한 클래스가 AccommodationFeatureId 라서, 기본키 타입 자리에 그 클래스를 적는다.
 */
public interface AccommodationFeatureRepository extends JpaRepository<AccommodationFeature, AccommodationFeatureId> {

    /**
     * 한 숙소가 가진 편의시설(Feature)들을 편의시설 번호 순서대로 가져온다.
     *
     * [조회문 읽는 법]
     * - select af.feature                 : 연결 행 자체가 아니라, 그 행이 가리키는 편의시설(Feature)을 꺼낸다.
     *     이유: 응답에 필요한 것은 편의시설의 이름·코드·분류이지 연결 정보가 아니다.
     * - from AccommodationFeature af      : 연결 엔티티를 af 라는 별명으로 조회한다.
     * - where af.accommodation.id = :accommodationId : 이 숙소의 연결 행만.
     * - order by af.feature.id asc        : 편의시설 번호가 작은 것부터. (조회할 때마다 순서가 같도록)
     */
    @Query("select af.feature from AccommodationFeature af where af.accommodation.id = :accommodationId order by af.feature.id asc")
    List<Feature> findFeaturesByAccommodationId(@Param("accommodationId") Long accommodationId);
}
