package com.kdt.reservation.auth.jwt;

import com.kdt.reservation.auth.TokenFailedException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            try {
                String token = header.substring(BEARER_PREFIX.length());

                // 판독기로 검사 (access 종류 확인까지 여기서 끝남)
                Claims claims = jwtProvider.parseAccessToken(token);

                Long userId = jwtProvider.getUserId(claims);
                String role = jwtProvider.getRole(claims);

                // 누구인지(userId) + 권한(ROLE_GUEST 등)
                UsernamePasswordAuthenticationToken authentication =
                        UsernamePasswordAuthenticationToken.authenticated(
                                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));

                // 요청 보관함이라 생각
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (TokenFailedException ex) {
                // 가짜, 만료 → 기록 없이 통과 (규칙에서 401 처리)
                SecurityContextHolder.clearContext();
            }
        }

        // 다음 단계로 넘김. 없으면 요청이 여기서 멈춤
        filterChain.doFilter(request, response);
    }
}