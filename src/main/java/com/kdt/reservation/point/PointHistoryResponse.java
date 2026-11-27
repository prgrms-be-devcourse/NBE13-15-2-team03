package com.kdt.reservation.point;

import java.time.LocalDateTime;

public record PointHistoryResponse(
        Long pointHistoryId,
        PointType type,
        int amount,
        Long reservationId,
        LocalDateTime createdAt
) {
    public static PointHistoryResponse from(PointHistory history) {
        Long reservationId = history.getReservation() == null ? null : history.getReservation().getId();
        return new PointHistoryResponse(
                history.getId(),
                history.getPointType(),
                history.getAmount(),
                reservationId,
                history.getCreated_at()
        );
    }
}
