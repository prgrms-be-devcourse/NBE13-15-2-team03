package com.kdt.reservation.auth.dto;

/**
 * 서비스 → 컨트롤러 전달용 (응답으로 직접 나가지 않음)
 */
public record ReissueResult(ReissueTokenResponse response, String refreshToken) {
}