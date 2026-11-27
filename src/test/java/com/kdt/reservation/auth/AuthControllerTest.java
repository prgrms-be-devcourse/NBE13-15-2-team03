package com.kdt.reservation.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kdt.reservation.auth.dto.CreateUserRequest;
import com.kdt.reservation.auth.dto.CreateUserResponse;
import com.kdt.reservation.auth.dto.LoginRequest;
import com.kdt.reservation.auth.dto.LoginResponse;
import com.kdt.reservation.auth.dto.LoginResult;                    // ★
import com.kdt.reservation.auth.dto.ReissueResult;                  // ★
import com.kdt.reservation.auth.dto.ReissueTokenResponse;           // ★
import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.user.Role;
import jakarta.servlet.http.Cookie;                                 // ★
import java.util.List;                                              // ★
import org.junit.jupiter.api.AfterEach;                             // ★
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;               // ★
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;   // ★
import org.springframework.security.core.authority.SimpleGrantedAuthority;                // ★
import org.springframework.security.core.context.SecurityContextHolder;                   // ★
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * AuthController 테스트 (회원가입, 로그인, 재발급, 로그아웃)
 *
 * @Import(RefreshTokenCookieProvider.class): 컨트롤러가 쿠키 도구를 쓰는데 @WebMvcTest 는 그걸 자동으로 안 띄워서
 *                                           직접 넣어 준다. 진짜 도구라 실제 쿠키 설정까지 검사된다
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RefreshTokenCookieProvider.class)                           // ★
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private static final String VALID_BODY = """
            {
              "loginId": "traveler01",
              "password": "pass1234word",
              "name": "김여행",
              "nickname": "여행자",
              "phone": "01012345678",
              "birthDate": "1998-05-17",
              "role": "GUEST"
            }
            """;

    // ★ 로그아웃 테스트에서 보관함에 넣은 로그인 정보를 테스트마다 비운다
    @AfterEach
    void clearLogin() {
        SecurityContextHolder.clearContext();
    }

    // ===== 회원가입 (변경 없음) =====

    @Test
    @DisplayName("회원가입 성공 시 201과 userId를 돌려준다")
    void signUp_success() throws Exception {
        given(authService.signUp(any(CreateUserRequest.class)))
                .willReturn(new CreateUserResponse(1L));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.userId").value(1));
    }

    @Test
    @DisplayName("아이디가 중복이면 409와 DUPLICATE_LOGIN_ID를 돌려준다")
    void signUp_duplicateLoginId() throws Exception {
        given(authService.signUp(any(CreateUserRequest.class)))
                .willThrow(new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_LOGIN_ID"));
    }

    @Test
    @DisplayName("아이디에 대문자가 있으면 400을 돌려주고 서비스를 부르지 않는다")
    void signUp_invalidLoginId() throws Exception {
        String body = VALID_BODY.replace("traveler01", "Traveler01");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));

        verify(authService, never()).signUp(any());
    }

    @Test
    @DisplayName("비밀번호에 숫자가 없으면 400을 돌려준다")
    void signUp_invalidPassword() throws Exception {
        String body = VALID_BODY.replace("pass1234word", "passwordonly");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));

        verify(authService, never()).signUp(any());
    }

    @Test
    @DisplayName("생년월일이 없으면 400을 돌려준다")
    void signUp_missingBirthDate() throws Exception {
        String body = VALID_BODY.replace("\"birthDate\": \"1998-05-17\",", "");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));

        verify(authService, never()).signUp(any());
    }

    // ===== 로그인 =====

    @Test
    @DisplayName("로그인 성공 시 본문에는 access 토큰, 쿠키에는 refresh 토큰을 담는다")
    void login_success() throws Exception {
        // ★ 변경: 서비스가 LoginResult(본문 + refresh 토큰)를 돌려준다
        given(authService.login(any(LoginRequest.class)))
                .willReturn(new LoginResult(
                        new LoginResponse("Bearer", "access-token", 1800,
                                new LoginResponse.UserInfo(1L, "여행자", Role.GUEST)),
                        "refresh-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "loginId": "traveler01", "password": "pass1234word" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.user.role").value("GUEST"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())            // ★ 본문에 refresh 없음
                .andExpect(cookie().value("refreshToken", "refresh-token"))          // ★ 쿠키에 refresh
                .andExpect(cookie().httpOnly("refreshToken", true))                  // ★ HttpOnly
                .andExpect(cookie().path("refreshToken", "/api/auth"));              // ★ Path
    }

    @Test
    @DisplayName("아이디 또는 비밀번호가 틀리면 401과 LOGIN_FAILED를 돌려준다")
    void login_fail() throws Exception {
        given(authService.login(any(LoginRequest.class)))
                .willThrow(new BusinessException(ErrorCode.LOGIN_FAILED));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "loginId": "traveler01", "password": "wrong1234" }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("LOGIN_FAILED"));
    }

    @Test
    @DisplayName("아이디나 비밀번호가 비어 있으면 400을 돌려준다")
    void login_blank() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "loginId": "", "password": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));

        verify(authService, never()).login(any());
    }

    // ===== 재발급 (★ 전부 새로 추가) =====

    @Test
    @DisplayName("재발급 성공 시 새 access 토큰과 새 refresh 쿠키를 돌려준다")
    void reissue_success() throws Exception {
        // 쿠키의 "old-refresh" 가 서비스로 그대로 전달되는지도 같이 검사된다
        given(authService.reissue("old-refresh"))
                .willReturn(new ReissueResult(
                        new ReissueTokenResponse("Bearer", "new-access", 1800),
                        "new-refresh"));

        mockMvc.perform(post("/api/auth/reissue")
                        .cookie(new Cookie("refreshToken", "old-refresh")))   // 요청에 쿠키 붙이기
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(cookie().value("refreshToken", "new-refresh"));
    }

    @Test
    @DisplayName("refresh 토큰이 유효하지 않으면 401과 INVALID_REFRESH_TOKEN을 돌려준다")
    void reissue_invalidToken() throws Exception {
        given(authService.reissue(any()))
                .willThrow(new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        mockMvc.perform(post("/api/auth/reissue")
                        .cookie(new Cookie("refreshToken", "bad-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    @DisplayName("refresh 쿠키가 없으면 401을 돌려준다")
    void reissue_noCookie() throws Exception {
        // 쿠키가 없으면 컨트롤러는 null 을 넘긴다 (required = false)
        given(authService.reissue(isNull()))
                .willThrow(new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        mockMvc.perform(post("/api/auth/reissue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_REFRESH_TOKEN"));
    }

    // ===== 로그아웃 (★ 새로 추가) =====

    @Test
    @DisplayName("로그아웃 시 저장소에서 삭제하고 refresh 쿠키를 지운다")
    void logout_success() throws Exception {
        // 필터가 꺼져 있으니 보관함에 1번 회원을 직접 넣는다
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_GUEST"))));

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(cookie().maxAge("refreshToken", 0));   // 수명 0 쿠키 = 삭제

        verify(authService).logout(1L);   // 1번 회원으로 로그아웃이 호출됐는지
    }
}