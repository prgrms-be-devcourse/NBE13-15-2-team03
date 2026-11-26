package com.kdt.reservation.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String loginId,
        @NotBlank String password
) {
}