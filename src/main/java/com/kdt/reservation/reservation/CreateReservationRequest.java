package com.kdt.reservation.reservation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateReservationRequest(
        @NotNull Long roomId,
        @NotNull LocalDateTime checkInDate,
        @NotNull LocalDateTime checkOutDate,
        @NotNull @Size(min = 1) int guestCount
        ) {
}
