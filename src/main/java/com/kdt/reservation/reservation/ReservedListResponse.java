package com.kdt.reservation.reservation;

import com.kdt.reservation.reservation.dto.GuestSummary;
import com.kdt.reservation.reservation.dto.RoomSummary;

import java.time.LocalDate;
import java.time.LocalDateTime;

//숙소의 예약목록 응답 DTO
public record ReservedListResponse(
        Long reservationId,
        Status status,
        RoomSummary room,
        GuestSummary guest,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int guestCount,
        int totalPrice,
        LocalDateTime createdAt
) {
}
