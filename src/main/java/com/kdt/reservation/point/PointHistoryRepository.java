package com.kdt.reservation.point;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    Optional<PointHistory> findByReservationIdAndPointType(Long reservationId, PointType pointType);

    boolean existsByReservationIdAndPointType(Long reservationId, PointType pointType);


    @Query("""
            select ph from PointHistory ph
            where ph.user.id = :userId
            and (:type is null or ph.pointType = :type)
            order by ph.created_at desc, ph.id desc
            """)
    Page<PointHistory> findMyHistory(
            @Param("userId") Long userId,
            @Param("type") PointType type,
            Pageable pageable
    );
}
