package com.kdt.reservation.reservation;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation,Long> {



    @Query(value = "SELECT r FROM Reservation r JOIN FETCH r.room p JOIN FETCH p.accommodation " +
            "WHERE r.user.id =:userId " +
            "AND r.status = :status ORDER BY r.created_at DESC" ,
        countQuery = "SELECT count(r) FROM Reservation r WHERE r.user.id =:userId " +
                "AND (:status is null or r.status = :status)")
    Page<Reservation> findAllByUserId(@Param("userId")Long userId,
                                      @Param("status")Status status,
                                      Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Reservation> findWithLockById(Long id);

}
