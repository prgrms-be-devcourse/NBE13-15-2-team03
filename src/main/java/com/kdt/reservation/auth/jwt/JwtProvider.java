package com.kdt.reservation.auth.jwt;

import com.kdt.reservation.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
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
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    // @Value: application.yml 의 값을 주입받는다. 값이 없으면 앱이 시작할 때 에러가 난다
    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.access-expiration-ms}") long accessExpirationMs,
                       @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String createAccessToken(Long userId, Role role) {
        return createToken(userId, role, TYPE_ACCESS, accessExpirationMs);
    }

    // refresh 토큰에는 role 을 안 넣는다. 재발급할 때 DB 에서 최신 역할을 다시 읽기 위해서
    public String createRefreshToken(Long userId) {
        return createToken(userId, null, TYPE_REFRESH, refreshExpirationMs);
    }

    private String createToken(Long userId, Role role, String type, long expirationMs) {
        // [실습 2] 토큰 만들기
        // 힌트: Jwts.builder() 에 아래를 차례로 붙인다
        //   .subject(String)        회원 번호 (String.valueOf(userId))
        //   .claim(키, 값)           CLAIM_TYPE -> type
        //   .issuedAt(Date)         지금 (new Date())
        //   .expiration(Date)       지금 + expirationMs
        //   role 이 null 이 아닐 때만 .claim(CLAIM_ROLE, role.name())
        //   .signWith(key)          서명
        //   .compact()              문자열로 완성
        // builder 를 JwtBuilder 변수에 담으면 if 문으로 role 을 조건부로 넣기 쉽다
        throw new UnsupportedOperationException("TODO 실습 2");
    }

    /**
     * 서명과 만료 시각을 검증하고 payload(Claims)를 돌려준다.
     * 위조/만료/형식 오류면 JwtException 이 발생한다 (잡는 건 호출하는 쪽에서)
     */
    public Claims parse(String token) {
        // [실습 3] 힌트: Jwts.parser() -> .verifyWith(key) -> .build()
        //              -> .parseSignedClaims(token) -> .getPayload()
        // 이 한 줄이 서명 검증 + 만료 검사를 전부 한다
        throw new UnsupportedOperationException("TODO 실습 3");
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
