package com.kdt.reservation.reservation;

import java.time.LocalDateTime;

public record CreateReservationResponse(
        Long reservationId,
        Status status,
        int totalPrice,
        int nights,
        LocalDateTime paymentDeadline
) {
}
