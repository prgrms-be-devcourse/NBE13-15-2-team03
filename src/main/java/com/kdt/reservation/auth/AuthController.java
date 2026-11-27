package com.kdt.reservation.auth;

import com.kdt.reservation.auth.dto.*;
import com.kdt.reservation.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 API
 * signup, login, reissue 는 SecurityConfig 에서 permitAll / logout 은 로그인 필요
 *
 * 쿠키는 HttpServletResponse 에 Set-Cookie 헤더로 직접 추가한다.
 * (ApiResponse 로는 헤더를 붙일 수 없어서)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieProvider cookieProvider;

    @PostMapping("/signup")
    public ApiResponse<CreateUserResponse> signup(@Valid @RequestBody CreateUserRequest request) {
        // 공통 응답 포맷: { "status": 201, "data": { "userId": 1 } }
        return ApiResponse.created(authService.signUp(request));
    }

    // 성공 시 200 + 토큰. /api/auth/login 은 SecurityConfig 에서 permitAll
    // 본문 : access 토큰 / 쿠키: refresh 토큰
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResult result = authService.login((request));
        response.addHeader(HttpHeaders.SET_COOKIE, cookieProvider.create(result.refreshToken()).toString());
        return ApiResponse.success(result.response());
    }

    /**
     * 토큰 재발급 (api-spec 3.3)
     *
     * @CookieValue: 요청에 붙어 온 쿠키에서 값을 꺼낸다
     * required = false: 쿠키가 없을 때 스프링 기본 400 대신, 서비스에서 INVALID_REFRESH_TOKEN(401)으로 처리하려고
     */
    @PostMapping("/reissue")
    public ApiResponse<ReissueTokenResponse> reissue(
            @CookieValue(name = RefreshTokenCookieProvider.COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response){
        ReissueResult result = authService.reissue(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieProvider.create(result.refreshToken()).toString());
        return ApiResponse.success(result.response());
    }


    /**
     * 로그아웃 (api-spec 3.4)
     * 서버 저장소의 refresh 삭제 + 브라우저 쿠키 삭제. 응답 data 는 null
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal Long userId, HttpServletResponse response){
        authService.logout(userId);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieProvider.expire().toString());
        return ApiResponse.success();
    }
}
