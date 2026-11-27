package com.kdt.reservation.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Refresh 토큰 쿠키를 만들고 지우는 도구 (Refresh 토큰은 HttpOnly 쿠키로 전달)
 *
 * 쿠키 설정을 한 곳에 모아 둔다. 로그인, 재발급, 로그아웃이 모두 같은 설정을 써야 해서.
 * (Path 나 이름이 하나라도 다르면 브라우저가 다른 쿠키로 취급해서 삭제가 안 된다)
 */
@Component
public class RefreshTokenCookieProvider {

    public static final String COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/api/auth";   // 재발급, 로그아웃 주소에만 쿠키가 따라간다

    private final long maxAgeSeconds;
    private final boolean secure;

    public RefreshTokenCookieProvider(@Value("${kdt.jwt.refresh-seconds}") long maxAgeSeconds,
                                      @Value("${kdt.jwt.cookie-secure:false}") boolean secure) {
        this.maxAgeSeconds = maxAgeSeconds;
        this.secure = secure;
    }

    // 로그인, 재발급 때: refresh 토큰을 담은 쿠키
    public ResponseCookie create(String refreshToken) {
        return ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)          // 자바스크립트가 못 읽음 (XSS 로 못 훔침)
                .secure(secure)          // true 면 https 에서만 전송
                .sameSite("Strict")      // 다른 사이트에서 보낸 요청엔 안 붙음 (CSRF 방어)
                .path(COOKIE_PATH)
                .maxAge(maxAgeSeconds)   // refresh 토큰 수명과 같게 (14일)
                .build();
    }

    // 로그아웃 때: 같은 이름·경로로 수명 0 인 쿠키를 보내면 브라우저가 지운다
    public ResponseCookie expire() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
    }
}