package com.kdt.reservation.room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 객실 이미지(room_image) 테이블에서 데이터를 꺼내고 저장하는 창구.
 */
public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {

    /**
     * 한 숙소에 속한 모든 객실의 이미지를 "한 번의 조회"로 가져온다. (삭제된 객실의 이미지는 제외)
     *
     * [객실마다 따로 조회하지 않고 한 번에 가져오는 이유]
     * 객실이 10개인 숙소에서 객실마다 이미지를 따로 조회하면 DB 조회가 10번 나간다.
     * 객실이 많아질수록 느려지는 이 문제를 흔히 "N+1 문제"라고 부른다.
     * 그래서 숙소 기준으로 한 번에 모두 가져온 뒤, 서비스에서 객실별로 나눠 담는다.
     *
     * [조회문 읽는 법]
     * - select i from RoomImage i                        : RoomImage 엔티티를 i 라는 별명으로 조회한다.
     * - where i.room.accommodation.id = :accommodationId : "이미지 -> 객실 -> 숙소" 로 따라가서, 이 숙소의 객실에 달린 이미지만.
     * - and i.room.deleted_at is null                    : 삭제된 객실의 이미지는 뺀다.
     * - order by i.id asc                                : id 가 작은 것부터. (첫 번째 이미지가 대표 이미지)
     */
    @Query("select i from RoomImage i where i.room.accommodation.id = :accommodationId and i.room.deleted_at is null order by i.id asc")
    List<RoomImage> findByAccommodationId(@Param("accommodationId") Long accommodationId);
}
