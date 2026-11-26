package com.kdt.reservation.user;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
// ★ 삭제: SecurityMockMvcRequestPostProcessors.authentication import

import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.user.dto.GetUserResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;                    // ★ 추가
import org.junit.jupiter.api.BeforeEach;                   // ★ 추가
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;   // ★ 추가
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * UserController 테스트 (내 정보 조회)
 *
 * addFilters = false 로 시큐리티 필터를 껐기 때문에, 필터가 해 주던 "보관함에 회원 넣기"를
 * 테스트가 직접 한다 (@BeforeEach). 넣어 둔 principal(1L)이 @AuthenticationPrincipal Long userId 로 들어간다.
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // 회원번호 1번, GUEST 로 로그인한 상태
    private static final UsernamePasswordAuthenticationToken LOGIN_USER_1 =
            new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_GUEST")));

    // ★ 추가: 각 테스트 시작 전에 보관함에 로그인한 회원을 넣는다 (필터가 하던 일)
    @BeforeEach
    void setUpLogin() {
        SecurityContextHolder.getContext().setAuthentication(LOGIN_USER_1);
    }

    // ★ 추가: 테스트가 끝나면 보관함을 비운다. 안 비우면 다른 테스트에 로그인 상태가 남는다
    @AfterEach
    void clearLogin() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("내 정보 조회 성공 시 200과 회원 정보를 돌려준다")
    void getMe_success() throws Exception {
        given(userService.getMe(1L)).willReturn(new GetUserResponse(
                1L, "traveler01", "김여행", "여행자", "01012345678",
                LocalDate.of(1998, 5, 17), Role.GUEST, List.of(),
                LocalDateTime.of(2026, 11, 25, 9, 0)));

        mockMvc.perform(get("/api/users/me"))                      // ★ 변경: .with(authentication(...)) 삭제
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.loginId").value("traveler01"))
                .andExpect(jsonPath("$.data.role").value("GUEST"))
                .andExpect(jsonPath("$.data.socialProviders").isEmpty())
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("토큰 속 회원이 없거나 탈퇴했으면 401을 돌려준다")
    void getMe_userNotFound() throws Exception {
        given(userService.getMe(1L)).willThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        mockMvc.perform(get("/api/users/me"))                      // ★ 변경: .with(authentication(...)) 삭제
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }
}