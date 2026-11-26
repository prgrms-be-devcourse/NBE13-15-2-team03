package com.kdt.reservation.user;

import com.kdt.reservation.common.response.ApiResponse;
import com.kdt.reservation.user.dto.GetUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}