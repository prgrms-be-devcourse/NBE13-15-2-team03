package com.kdt.reservation.auth.security;

import com.kdt.reservation.auth.jwt.JwtAuthenticationFilter;
import com.kdt.reservation.auth.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 토큰을 헤더로 보내므로 CSRF 방어 불필요
                .csrf(AbstractHttpConfigurer::disable)
                // 스프링 기본 로그인 화면, 브라우저 로그인 팝업 끄기
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 세션 안 만듦. 매 요청 토큰만 보고 판단
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // TODO CORS: 프론트 주소가 정해지면 설정

                .authorizeHttpRequests(auth -> auth
                        // ===== 공개 =====
                        // 인증 API 중 로그인 전에 호출하는 것 (logout 은 로그인 필요라 제외)
                        .requestMatchers("/api/auth/signup", "/api/auth/login", "/api/auth/reissue").permitAll()
                        // 스프링 내부 에러 경로. 안 열면 404 같은 에러가 전부 401 로 보임
                        .requestMatchers("/error").permitAll()
                        // 소셜 로그인 기본 경로
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                        // ===== 숙소, 객실 (B) =====
                        // 위에서부터 검사하므로 좁은 규칙(me)을 넓은 규칙(*)보다 먼저
                        .requestMatchers(HttpMethod.GET, "/api/accommodations/me").hasRole("HOST")
                        .requestMatchers(HttpMethod.GET, "/api/accommodations/*/reservations").hasRole("HOST")
                        .requestMatchers(HttpMethod.GET,
                                "/api/accommodations",
                                "/api/accommodations/*",
                                "/api/accommodations/*/reserved-dates",
                                "/api/accommodations/*/reviews").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/features").permitAll()
                        // 숙소 등록·수정·삭제, 객실 등록·수정·삭제
                        .requestMatchers("/api/accommodations/**", "/api/rooms/**").hasRole("HOST")

                        // ===== 예약 (C) =====
                        .requestMatchers(HttpMethod.POST, "/api/reservations").hasRole("GUEST")
                        .requestMatchers(HttpMethod.GET, "/api/reservations/me").hasRole("GUEST")

                        // ===== 나머지 전부 로그인 필요 =====
                        // 회원(/api/users/me), 로그아웃, 예약 상세·취소, 결제, 포인트, 리뷰 작성·수정·삭제, 이미지 업로드
                        // "본인 것만" 검사는 각 서비스에서 한다
                        .anyRequest().authenticated())

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)  // 401
                        .accessDeniedHandler(accessDeniedHandler))           // 403

                // 스프링 기본 로그인 필터보다 앞에 JWT 필터 배치
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);

        // TODO 소셜 로그인: oauth2-client 의존성, 설정 추가 후
        // http.oauth2Login(...)

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}