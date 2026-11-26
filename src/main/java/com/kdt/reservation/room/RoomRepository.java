package com.kdt.reservation.room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room,Long> {

    /**
     * 한 숙소에 속한 "삭제되지 않은" 객실들을 번호(id) 순서대로 가져온다.
     *
     * [@Query 를 쓴 이유]
     * 엔티티 필드 이름에 밑줄(deleted_at)이 있어서 메서드 이름만으로는 조회문이 만들어지지 않는다.
     * 그래서 조회문(JPQL)을 직접 적었다. (AccommodationRepository 와 같은 이유)
     *
     * [조회문 읽는 법]
     * - select r from Room r                        : Room 엔티티를 r 이라는 별명으로 조회한다.
     * - where r.accommodation.id = :accommodationId : 이 숙소에 속한 객실만.
     * - and r.deleted_at is null                    : 삭제되지 않은 객실만. (soft delete)
     *     주의: "판매 중지(is_available = false)" 객실은 걸러 내지 않는다.
     *     이유: 명세 6.2 에서 판매 중지 객실도 상세 화면에는 보여 주기로 했기 때문이다.
     *           (예약만 막는다. 예약을 막는 일은 예약 API 쪽에서 한다.)
     * - order by r.id asc                           : 번호가 작은 것부터.
     *     이유: 정렬을 적지 않으면 조회할 때마다 순서가 달라질 수 있어 화면이 들쭉날쭉해진다.
     */
    @Query("select r from Room r where r.accommodation.id = :accommodationId and r.deleted_at is null order by r.id asc")
    List<Room> findActiveByAccommodationId(@Param("accommodationId") Long accommodationId);
}
