package com.kdt.reservation.auth.jwt;

import com.kdt.reservation.auth.TokenFailedException;
import com.kdt.reservation.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class JwtProvider {

    // refresh 토큰으로 일반 API 를 호출하지 못하게, 토큰에 종류를 적어 둔다
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    // 로그인 응답의 accessTokenExpiresIn 에 쓴다
    @Getter
    private final long accessSeconds;
    private final long refreshSeconds;

    // @Value: application.yml 의 값을 주입받는다. 값이 없으면 앱이 시작할 때 에러가 난다
    public JwtProvider(@Value("${kdt.jwt.secret}") String secret,
                       @Value("${kdt.jwt.access-seconds}") long accessSeconds,
                       @Value("${kdt.jwt.refresh-seconds}") long refreshSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessSeconds = accessSeconds;
        this.refreshSeconds = refreshSeconds;
    }

    public String createAccessToken(Long userId, Role role) {
        return createToken(userId, role, TYPE_ACCESS, accessSeconds);
    }

    // refresh 토큰에는 role 을 안 넣는다. 재발급할 때 DB 에서 최신 역할을 다시 읽기 위해서
    public String createRefreshToken(Long userId) {
        return createToken(userId, null, TYPE_REFRESH, refreshSeconds);
    }

    private String createToken(Long userId, Role role, String type, long expireSeconds) {
        Instant now = Instant.now();
        JwtBuilder jwtBuilder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)));
                if (role != null){
                    jwtBuilder.claim(CLAIM_ROLE, role.name());
                }

        return jwtBuilder.signWith(key).compact();
    }


    // 필터에서 쓸 것: access 토큰만 통과
    public Claims parseAccessToken(String token) {
        return parseWithType(token, TYPE_ACCESS);
    }

    // 재발급 API 에서 쓸 것: refresh 토큰만 통과
    public Claims parseRefreshToken(String token) {
        return parseWithType(token, TYPE_REFRESH);
    }

    private Claims parseWithType(String token, String expectedType) {
        if (token == null || token.isBlank()) {
            throw new TokenFailedException();
        }

        try {
            Claims claims = Jwts.parser().verifyWith(this.key).build()
                    .parseSignedClaims(token).getPayload();

            Object type = claims.get(CLAIM_TYPE);
            if (type == null || !expectedType.equals(type.toString())) {
                throw new TokenFailedException();
            }

            if (claims.getSubject() == null || claims.getSubject().isBlank() || claims.getExpiration() == null) {
                throw new TokenFailedException();
            }

            return claims;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new TokenFailedException();
        }
    }

    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public String getRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }

    public String getType(Claims claims) {
        return claims.get(CLAIM_TYPE, String.class);
    }
}
