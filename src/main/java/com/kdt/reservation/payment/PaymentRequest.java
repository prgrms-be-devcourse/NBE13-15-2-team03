package com.kdt.reservation.payment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
        @NotNull PayMethod method,
        @Min(0) int usePoint
) {
}
