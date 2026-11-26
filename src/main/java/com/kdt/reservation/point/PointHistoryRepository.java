package com.kdt.reservation.point;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    Optional<PointHistory> findByReservationIdAndPointType(Long reservationId, PointType pointType);

    boolean existsByReservationIdAndPointType(Long reservationId, PointType pointType);
}
