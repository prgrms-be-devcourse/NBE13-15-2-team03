package com.kdt.reservation.user;

import com.kdt.reservation.common.response.ApiResponse;
import com.kdt.reservation.user.dto.ChangePasswordRequest;
import com.kdt.reservation.user.dto.GetUserResponse;
import com.kdt.reservation.user.dto.UpdateUserRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 회원 API (/api/users/me)
 *
 * SecurityConfig 에서 /api/users/** 는 따로 열지 않아서 anyRequest().authenticated() 에 걸린다
 * → 토큰 없으면 컨트롤러까지 오지도 않고 401
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회 (api-spec 5.1)
     * @AuthenticationPrincipal Long userId
     *   → JwtAuthenticationFilter 가 토큰에서 꺼내 보관함에 넣은 회원 번호
     *   → request 로 받지 않는다 (남의 번호를 넣어 보낼 수 있어서)
     */
    @GetMapping
    public ApiResponse<GetUserResponse> getMe(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getMe(userId));
    }

    // 내 정보 수정 (api-spec 5.2). 응답은 수정된 내 정보
    @PatchMapping
    public ApiResponse<GetUserResponse> updateMe(@AuthenticationPrincipal Long userId,
                                                 @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(userService.updateMe(userId, request));
    }

    // 비밀번호 변경 (api-spec 5.3). 응답 data 는 null
    @PatchMapping("/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal Long userId,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        return ApiResponse.success();
    }
}