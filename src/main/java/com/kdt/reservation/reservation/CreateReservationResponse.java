package com.kdt.reservation.reservation;

import java.time.LocalDateTime;

public record CreateReservationResponse(
        Long reservationId,
        Status status,
        int totalPrice,
        int nights,
        LocalDateTime paymentDeadline
) {
    public static CreateReservationResponse from(Reservation reservation,
                                                int nights,LocalDateTime deadline){
        return new CreateReservationResponse(
                reservation.getId(),
                reservation.getStatus(),
                reservation.getTotal_price(),
                nights,
                deadline
        );
    }
}
