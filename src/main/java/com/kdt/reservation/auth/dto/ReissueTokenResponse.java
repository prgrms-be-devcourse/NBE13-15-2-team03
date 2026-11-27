package com.kdt.reservation.auth.dto;

/**
 * 재발급 응답 본문 (api-spec 3.3: 로그인 응답에서 user 를 뺀 것)
 * 새 Refresh 토큰은 쿠키로 보낸다
 */
public record ReissueTokenResponse(
        String tokenType,
        String accessToken,
        long accessTokenExpiresIn
) {
}