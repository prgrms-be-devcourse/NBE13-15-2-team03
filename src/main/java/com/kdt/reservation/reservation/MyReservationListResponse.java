package com.kdt.reservation.reservation;

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
    public static MyReservationListResponse from(Reservation reservation){
        return new MyReservationListResponse(
                reservation.getId(),
                reservation.getStatus(),

                reservation.getCheck_in_date(),
                reservation.getCheck_out_date(),
                reservation.getGuest_count(),
                reservation.getTotal_price(),

                reservation.getCreated_at()
        )
    }
}
