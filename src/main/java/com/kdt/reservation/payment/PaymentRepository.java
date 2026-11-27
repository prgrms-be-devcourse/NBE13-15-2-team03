package com.kdt.reservation.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment,Long> {

    @Query("""
                SELECT p
                FROM Payment p
                WHERE p.reservation.id =: reservationId
                """)
    public Optional<Payment> findByReservationId(@Param("reservationId")Long reservationId);
}
