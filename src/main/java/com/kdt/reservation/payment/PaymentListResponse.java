package com.kdt.reservation.payment;

import java.time.LocalDateTime;

public record PaymentListResponse(
        Long paymentId,
        PayMethod method,
        PayStatus status,
        int amount,
        int usedPoint,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime cancelledAt
) {
}
