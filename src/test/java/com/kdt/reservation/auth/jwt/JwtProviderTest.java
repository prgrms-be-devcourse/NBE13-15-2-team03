package com.kdt.reservation.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kdt.reservation.auth.TokenFailedException;
import com.kdt.reservation.user.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * JwtProvider 단위 테스트
 *
 * 스프링을 띄우지 않고 new 로 직접 만들어서, 토큰 생성·검증 로직이 실제로 실행된다.
 * (컨트롤러 테스트는 서비스를 가짜로 바꿔서 JwtProvider 가 실행되지 않는다)
 *
 * assertThat(실제값).isEqualTo(기대값)        : 값이 같은지
 * assertThatThrownBy(() -> 코드).isInstanceOf(예외) : 그 코드가 예외를 던지는지
 */
class JwtProviderTest {

    // HS256 은 키가 32바이트 이상이어야 한다. 테스트 전용 값이라 아무 문자열이나 길게
    private static final String SECRET = "test-secret-key-for-jwt-provider-unit-test-0123456789";

    // access 30분, refresh 14일 (application.yaml 과 같은 값)
    private final JwtProvider jwtProvider = new JwtProvider(SECRET, 1800, 1209600);

    // ===== jti (이번 PR 에서 추가한 부분) =====

    @Test
    @DisplayName("같은 회원의 refresh 토큰을 연달아 만들어도 서로 다른 토큰이 나온다")
    void sameUser_differentTokens() {
        // 같은 1초 안에 두 번 만든다 (jti 가 없으면 완전히 같은 문자열이 나오던 경우)
        String first = jwtProvider.createRefreshToken(1L);
        String second = jwtProvider.createRefreshToken(1L);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("토큰마다 고유한 jti 가 들어간다")
    void eachToken_hasUniqueJti() {
        Claims first = jwtProvider.parseRefreshToken(jwtProvider.createRefreshToken(1L));
        Claims second = jwtProvider.parseRefreshToken(jwtProvider.createRefreshToken(1L));

        // getId() 가 jti 값을 꺼낸다
        assertThat(first.getId()).isNotBlank();
        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    // ===== 정상 발급·검증 (jti 추가 후에도 기존 동작이 그대로인지) =====

    @Test
    @DisplayName("access 토큰에는 회원번호, 역할, 종류(access)가 들어 있다")
    void accessToken_containsUserIdRoleType() {
        String token = jwtProvider.createAccessToken(7L, Role.GUEST);

        Claims claims = jwtProvider.parseAccessToken(token);

        assertThat(jwtProvider.getUserId(claims)).isEqualTo(7L);
        assertThat(jwtProvider.getRole(claims)).isEqualTo("GUEST");
        assertThat(jwtProvider.getType(claims)).isEqualTo(JwtProvider.TYPE_ACCESS);
    }

    @Test
    @DisplayName("refresh 토큰에는 회원번호와 종류(refresh)만 있고 역할은 없다")
    void refreshToken_hasNoRole() {
        String token = jwtProvider.createRefreshToken(7L);

        Claims claims = jwtProvider.parseRefreshToken(token);

        assertThat(jwtProvider.getUserId(claims)).isEqualTo(7L);
        assertThat(jwtProvider.getType(claims)).isEqualTo(JwtProvider.TYPE_REFRESH);
        assertThat(jwtProvider.getRole(claims)).isNull();   // 재발급 때 DB 에서 최신 역할을 읽기 위해 안 넣음
    }

    // ===== 종류(type) 검사 =====

    @Test
    @DisplayName("refresh 토큰으로는 access 검사를 통과할 수 없다 (일반 API 호출 불가)")
    void refreshToken_rejectedAsAccess() {
        String refresh = jwtProvider.createRefreshToken(1L);

        assertThatThrownBy(() -> jwtProvider.parseAccessToken(refresh))
                .isInstanceOf(TokenFailedException.class);
    }

    @Test
    @DisplayName("access 토큰으로는 refresh 검사를 통과할 수 없다 (재발급 불가)")
    void accessToken_rejectedAsRefresh() {
        String access = jwtProvider.createAccessToken(1L, Role.GUEST);

        assertThatThrownBy(() -> jwtProvider.parseRefreshToken(access))
                .isInstanceOf(TokenFailedException.class);
    }

    // ===== 위조·만료 =====

    @Test
    @DisplayName("다른 회원 토큰의 내용(payload)을 바꿔 끼우면 서명이 안 맞아 거부된다")
    void tamperedPayload_rejected() {
        // 토큰 구조: header.payload.signature
        String[] mine = jwtProvider.createAccessToken(1L, Role.GUEST).split("\\.");
        String[] other = jwtProvider.createAccessToken(2L, Role.HOST).split("\\.");

        // 내 서명은 그대로 두고, 내용만 2번 회원(HOST)의 것으로 바꿔치기
        String forged = mine[0] + "." + other[1] + "." + mine[2];

        assertThatThrownBy(() -> jwtProvider.parseAccessToken(forged))
                .isInstanceOf(TokenFailedException.class);
    }

    @Test
    @DisplayName("다른 비밀키로 서명된 토큰은 거부된다")
    void differentSecret_rejected() {
        JwtProvider otherServer = new JwtProvider(
                "another-secret-key-that-is-also-long-enough-0123456789", 1800, 1209600);
        String token = otherServer.createAccessToken(1L, Role.GUEST);

        assertThatThrownBy(() -> jwtProvider.parseAccessToken(token))
                .isInstanceOf(TokenFailedException.class);
    }

    @Test
    @DisplayName("만료된 토큰은 거부된다")
    void expiredToken_rejected() {
        // 수명을 -1초로 만들면 만들자마자 이미 만료된 토큰이 된다
        JwtProvider expiredProvider = new JwtProvider(SECRET, -1, -1);
        String token = expiredProvider.createAccessToken(1L, Role.GUEST);

        assertThatThrownBy(() -> jwtProvider.parseAccessToken(token))
                .isInstanceOf(TokenFailedException.class);
    }

    @Test
    @DisplayName("빈 토큰이나 형식이 틀린 토큰은 거부된다")
    void blankOrMalformed_rejected() {
        assertThatThrownBy(() -> jwtProvider.parseAccessToken(null))
                .isInstanceOf(TokenFailedException.class);
        assertThatThrownBy(() -> jwtProvider.parseAccessToken(""))
                .isInstanceOf(TokenFailedException.class);
        assertThatThrownBy(() -> jwtProvider.parseAccessToken("abc.def.ghi"))
                .isInstanceOf(TokenFailedException.class);
    }
}