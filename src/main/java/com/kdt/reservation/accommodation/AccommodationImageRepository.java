package com.kdt.reservation.accommodation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 숙소 이미지(accommodation_image) 테이블에서 데이터를 꺼내고 저장하는 창구.
 *
 * [숙소 레포지토리와 따로 만든 이유]
 * 레포지토리는 "엔티티 하나당 하나"로 만드는 것이 기본이다.
 * 이미지는 AccommodationImage 라는 별도 엔티티(별도 테이블)라서 창구도 따로 둔다.
 */
public interface AccommodationImageRepository extends JpaRepository<AccommodationImage, Long> {

    /**
     * 한 숙소에 등록된 이미지 주소(URL)들을 id 가 작은 것부터 순서대로 가져온다.
     *
     * [조회문 읽는 법]
     * - select i.url                           : 이미지 엔티티 전체가 아니라 주소(url) 글자만 꺼낸다.
     *     이유: 응답에 필요한 것은 주소뿐이라, 필요한 값만 가져오는 편이 가볍다.
     * - from AccommodationImage i              : AccommodationImage 엔티티를 i 라는 별명으로 조회한다.
     * - where i.accommodation.id = :accommodationId : 이 숙소에 속한 이미지만.
     * - order by i.id asc                      : id 오름차순(작은 것부터)으로 정렬한다.
     *     이유: 팀 결정(명세 0-1의 22번)에 따라 "id 가 가장 작은 첫 번째 이미지 = 대표 이미지"다.
     *           정렬을 적지 않으면 DB 가 돌려주는 순서가 매번 같다고 보장되지 않는다.
     *
     * [반환 타입이 List<String> 인 이유]
     * 이미지는 여러 장일 수 있으므로 목록(List)으로 받는다.
     * 이미지가 한 장도 없으면 null 이 아니라 "빈 목록"이 온다. (명세: 없으면 빈 배열)
     */
    @Query("select i.url from AccommodationImage i where i.accommodation.id = :accommodationId order by i.id asc")
    List<String> findUrlsByAccommodationId(@Param("accommodationId") Long accommodationId);
}
