package com.kdt.reservation.reservation;

import java.time.LocalDateTime;

public record ReservationCancelledResponse(
        Long reservationId,
        Status status,
        LocalDateTime cancelledAt,
        RefundSummary refund
) {
    public record RefundSummary(
            int amount,
            int refundedPoint
    ){}
}
