package com.kdt.reservation.room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 객실-편의시설 연결(room_feature) 테이블에서 데이터를 꺼내고 저장하는 창구.
 *
 * 기본키가 (객실 번호, 편의시설 번호) 두 값의 묶음이라, 기본키 타입 자리에 RoomFeatureId 를 적는다.
 */
public interface RoomFeatureRepository extends JpaRepository<RoomFeature, RoomFeatureId> {

    /**
     * 한 숙소에 속한 모든 객실의 편의시설 연결 정보를 "한 번의 조회"로 가져온다. (삭제된 객실 제외)
     *
     * [객실마다 따로 조회하지 않는 이유]
     * 객실 이미지와 같은 이유다. 객실 수만큼 조회가 늘어나는 것(N+1 문제)을 막기 위해
     * 숙소 기준으로 한 번에 가져오고, 서비스에서 객실별로 나눠 담는다.
     *
     * [조회문 읽는 법]
     * - select rf from RoomFeature rf  : 연결 엔티티를 rf 라는 별명으로 조회한다.
     *     (연결 행을 통째로 가져오는 이유: "어느 객실의 것인지"도 알아야 객실별로 나눌 수 있다)
     * - join fetch rf.feature          : 연결된 편의시설(Feature)도 같은 조회에서 함께 가져온다.
     *     이유: feature 는 LAZY 설정이라, 이렇게 하지 않으면 이름을 꺼낼 때마다 DB 조회가 따로 나간다.
     * - where rf.room.accommodation.id = :accommodationId : "연결 -> 객실 -> 숙소"로 따라가서 이 숙소의 객실 것만.
     * - and rf.room.deleted_at is null : 삭제된 객실의 것은 뺀다.
     * - order by rf.feature.id asc     : 편의시설 번호가 작은 것부터.
     */
    @Query("select rf from RoomFeature rf join fetch rf.feature where rf.room.accommodation.id = :accommodationId and rf.room.deleted_at is null order by rf.feature.id asc")
    List<RoomFeature> findByAccommodationId(@Param("accommodationId") Long accommodationId);
}
