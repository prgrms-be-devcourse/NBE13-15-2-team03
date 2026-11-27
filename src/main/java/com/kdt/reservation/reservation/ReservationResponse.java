package com.kdt.reservation.reservation;

import com.kdt.reservation.payment.PayMethod;
import com.kdt.reservation.payment.PayStatus;
import com.kdt.reservation.payment.Payment;
import com.kdt.reservation.point.PointHistory;
import com.kdt.reservation.reservation.dto.GuestSummary;
import com.kdt.reservation.reservation.dto.RoomSummary;

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
    public static ReservationResponse from(Reservation reservation, int night,
                                           Payment payment, PointHistory pointHistory){
        return new ReservationResponse(
                reservation.getId(),
                reservation.getStatus(),
                new GuestSummary(reservation.getUser().getId(),reservation.getUser().getName()),
                new AccommodationSummary(reservation.getRoom().getAccommodation().getId(),
                        reservation.getRoom().getAccommodation().getName(),
                        reservation.getRoom().getAccommodation().getAddress()),
                new RoomSummary(reservation.getRoom().getId(),
                        reservation.getRoom().getName()),
                reservation.getCheck_in_date(),
                reservation.getCheck_out_date(),
                night,
                reservation.getGuest_count(),
                reservation.getTotal_price(),
                new PaymentSummary(payment.getId(),
                        payment.getPayMethod(),
                        payment.getPayStatus(),
                        payment.getAmount(),
                        pointHistory.getAmount(),
                        payment.getCreated_at(),
                        payment.getPaid_at(),
                        payment.getCancelled_at()),
                reservation.getCreated_at(),
                reservation.getCancelled_at()
        );
    }

    public record AccommodationSummary(
            Long accommodationId,
            String name,
            String address) {
    }

    public record PaymentSummary(
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
}
