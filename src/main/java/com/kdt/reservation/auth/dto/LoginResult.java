package com.kdt.reservation.auth.dto;

/**
 * 서비스 → 컨트롤러 전달용 (응답으로 직접 나가지 않음)
 * response: 응답 본문 / refreshToken: 컨트롤러가 쿠키에 담을 값
 */
public record LoginResult(LoginResponse response, String refreshToken) {
}