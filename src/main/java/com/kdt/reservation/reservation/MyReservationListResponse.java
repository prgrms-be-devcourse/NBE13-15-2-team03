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
    public record RoomSummary(Long roomId, String name){}
}
