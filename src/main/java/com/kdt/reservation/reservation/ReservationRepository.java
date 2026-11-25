package com.kdt.reservation.reservation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation,Long> {

    //(:status is null or r.status = :status) 이 쿼리는 Status에 null이 들어오면 결과가 0건이 나옵니다
    //전체를 보여줘야하는데 안나올수도있다고 합니다
    //status is null 만약 status가 null이면 참이기때문에 status에 따라 구분하지않고 모두 조회합니다.
    @Query(value = "SELECT r FROM Reservation r JOIN FETCH r.room p JOIN FETCH p.accommodation " +
            "WHERE r.user.id =:userId " +
            "AND (:status is null or r.status = :status) ORDER BY r.created_at DESC" ,
        countQuery = "SELECT count(r) FROM Reservation r WHERE r.user.id =:userId " +
                "AND (:status is null or r.status = :status)")
    Page<Reservation> findAllByUserId(@Param("userId")Long userId,
                                    @Param("status")Status status,
                                    Pageable pageable);
}
