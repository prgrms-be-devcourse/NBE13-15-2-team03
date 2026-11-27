package com.kdt.reservation.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.user.dto.ChangePasswordRequest;
import com.kdt.reservation.user.dto.GetUserResponse;
import com.kdt.reservation.user.dto.UpdateUserRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * UserController 테스트 (내 정보 조회, 수정, 비밀번호 변경)
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

    private static final UsernamePasswordAuthenticationToken LOGIN_USER_1 =
            new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_GUEST")));

    // ★ 여러 테스트에서 쓰는 응답 예시
    private static GetUserResponse sampleResponse(String nickname, String phone) {
        return new GetUserResponse(
                1L, "traveler01", "김여행", nickname, phone,
                LocalDate.of(1998, 5, 17), Role.GUEST, List.of(),
                LocalDateTime.of(2026, 11, 25, 9, 0));
    }

    @BeforeEach
    void setUpLogin() {
        SecurityContextHolder.getContext().setAuthentication(LOGIN_USER_1);
    }

    @AfterEach
    void clearLogin() {
        SecurityContextHolder.clearContext();
    }

    // ===== 내 정보 조회 (기존) =====

    @Test
    @DisplayName("내 정보 조회 성공 시 200과 회원 정보를 돌려준다")
    void getMe_success() throws Exception {
        given(userService.getMe(1L)).willReturn(sampleResponse("여행자", "01012345678"));

        mockMvc.perform(get("/api/users/me"))
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

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    // ===== ★ 내 정보 수정 =====

    @Test
    @DisplayName("내 정보 수정 성공 시 200과 수정된 정보를 돌려준다")
    void updateMe_success() throws Exception {
        given(userService.updateMe(eq(1L), any(UpdateUserRequest.class)))
                .willReturn(sampleResponse("새닉네임", "01012345678"));

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nickname": "새닉네임" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("새닉네임"));
    }

    @Test
    @DisplayName("전화번호가 다른 회원과 중복이면 409를 돌려준다")
    void updateMe_duplicatePhone() throws Exception {
        given(userService.updateMe(eq(1L), any(UpdateUserRequest.class)))
                .willThrow(new BusinessException(ErrorCode.DUPLICATE_PHONE));

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "phone": "01033334444" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_PHONE"));
    }

    @Test
    @DisplayName("전화번호 형식이 틀리면 400을 돌려주고 서비스를 부르지 않는다")
    void updateMe_invalidPhone() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "phone": "010-1234" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));

        verify(userService, never()).updateMe(any(), any());
    }

    // ===== ★ 비밀번호 변경 =====

    @Test
    @DisplayName("비밀번호 변경 성공 시 200을 돌려준다")
    void changePassword_success() throws Exception {
        mockMvc.perform(patch("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "currentPassword": "pass1234word", "newPassword": "newpass1234" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        verify(userService).changePassword(eq(1L), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 400 PASSWORD_MISMATCH를 돌려준다")
    void changePassword_mismatch() throws Exception {
        // 반환값이 없는(void) 메서드가 예외를 던지게 할 때는 willThrow(...).given(...) 순서로 쓴다
        willThrow(new BusinessException(ErrorCode.PASSWORD_MISMATCH))
                .given(userService).changePassword(eq(1L), any(ChangePasswordRequest.class));

        mockMvc.perform(patch("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "currentPassword": "wrong1234", "newPassword": "newpass1234" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PASSWORD_MISMATCH"));
    }

    @Test
    @DisplayName("소셜 전용 계정이면 400 PASSWORD_NOT_SET을 돌려준다")
    void changePassword_notSet() throws Exception {
        willThrow(new BusinessException(ErrorCode.PASSWORD_NOT_SET))
                .given(userService).changePassword(eq(1L), any(ChangePasswordRequest.class));

        mockMvc.perform(patch("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "currentPassword": "anything1", "newPassword": "newpass1234" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PASSWORD_NOT_SET"));
    }

    @Test
    @DisplayName("새 비밀번호에 숫자가 없으면 400을 돌려주고 서비스를 부르지 않는다")
    void changePassword_invalidNewPassword() throws Exception {
        mockMvc.perform(patch("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "currentPassword": "pass1234word", "newPassword": "onlyletters" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));

        verify(userService, never()).changePassword(any(), any());
    }
}