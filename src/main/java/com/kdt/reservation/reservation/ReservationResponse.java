package com.kdt.reservation.reservation;

import com.kdt.reservation.payment.PayMethod;
import com.kdt.reservation.payment.PayStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long reservationId,
        Status status,
        GuestSummary guest,
        AccommodationSummary accommodation,
        RoomSummary room,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int nights,
        int guestCount,
        int totalPrice,
        PaymentSummary payment,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt
) {
    public record GuestSummary (Long userId, String name){}
    public record AccommodationSummary(Long accommodationId,
               String name, String address){}
    public record RoomSummary(Long roomId, String name){}
    public record PaymentSummary(
            Long paymentId,
            PayMethod method,
            PayStatus status,
            int amount,
            int usedPoint,
            LocalDateTime createdAt,
            LocalDateTime paidAt,
            LocalDateTime cancelledAt
    ){}
}
