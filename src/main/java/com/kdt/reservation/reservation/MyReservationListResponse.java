package com.kdt.reservation.reservation;

import com.kdt.reservation.reservation.dto.RoomSummary;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MyReservationListResponse(
        Long reservationId,
        Status status,
        //accommodation.accommodationId
        //accommodation.name
        AccommodationSummary accommodation,
        //room.roomId
        //room.name
        RoomSummary room,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int guestCount,
        int totalPrice,
        boolean hasReview,
        LocalDateTime createdAt
) {
    public record AccommodationSummary(Long accommodationId, String name){}
    public static MyReservationListResponse from(Reservation reservation, boolean hasReview){
        return new MyReservationListResponse(
                reservation.getId(),
                reservation.getStatus(),
                new AccommodationSummary(reservation.getRoom().getAccommodation().getId(),
                        reservation.getRoom().getAccommodation().getName()),
                new RoomSummary(reservation.getRoom().getId(),
                        reservation.getRoom().getName()),
                reservation.getCheck_in_date(),
                reservation.getCheck_out_date(),
                reservation.getGuest_count(),
                reservation.getTotal_price(),
                hasReview,
                reservation.getCreated_at()
        );
    }
}
