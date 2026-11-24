package com.kdt.reservation.payment;

import com.kdt.reservation.reservation.Status;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        Long reservationId,
        Status reservationStatus,
        PayMethod method,
        PayStatus status,
        int amount,
        int usedPoint,
        int earnedPoint,
        LocalDateTime paidAt
) {
}
