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
    public static PaymentResponse from(Payment payment,int usePoint,int earnedPoint){
        return new PaymentResponse(
                payment.getId(),
                payment.getReservation().getId(),
                payment.getReservation().getStatus(),
                payment.getPayMethod(),
                payment.getPayStatus(),
                payment.getAmount(),
                usePoint,
                earnedPoint,
                payment.getPaid_at()
        );
    }
}
