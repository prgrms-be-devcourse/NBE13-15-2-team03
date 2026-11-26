package com.kdt.reservation.auth;

import com.kdt.reservation.auth.dto.CreateUserRequest;
import com.kdt.reservation.auth.dto.CreateUserResponse;
import com.kdt.reservation.auth.dto.LoginRequest;
import com.kdt.reservation.auth.dto.LoginResponse;
import com.kdt.reservation.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ApiResponse<CreateUserResponse> signup(@Valid @RequestBody CreateUserRequest request) {
        // 공통 응답 포맷: { "status": 201, "data": { "userId": 1 } }
        return ApiResponse.created(authService.signUp(request));
    }


    // 성공 시 200 + 토큰. /api/auth/login 은 SecurityConfig 에서 permitAll
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }
}
