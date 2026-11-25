package com.kdt.reservation.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kdt.reservation.auth.dto.CreateUserRequest;
import com.kdt.reservation.auth.dto.CreateUserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

/**
 * AuthController 테스트 (회원가입)
 *
 * @WebMvcTest(AuthController.class): 컨트롤러 하나만. 서비스, DB, 다른 컨트롤러는 안 띄운다
 * @AutoConfigureMockMvc(addFilters = false): 시큐리티 필터를 끈다. 여기선 컨트롤러 동작만 검사하기 위해서
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    // 가짜 HTTP 요청을 보내는 도구. 서버를 실제로 켜지 않고 컨트롤러를 호출한다
    @Autowired
    private MockMvc mockMvc;

    // 진짜 AuthService 대신 가짜를 넣는다. given(...) 으로 "이렇게 부르면 이렇게 답해라"를 정한다
    @MockitoBean
    private AuthService authService;

    // 정상 요청 본문 (api-spec 3.1 예시)
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

    @Test
    @DisplayName("회원가입 성공 시 201과 userId를 돌려준다")
    void signUp_success() throws Exception {
        // given: 서비스가 userId 1 을 돌려준다고 정함
        given(authService.signUp(any(CreateUserRequest.class)))
                .willReturn(new CreateUserResponse(1L));

        // when & then: 요청을 보내고 응답을 검사
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))        // 공통 포맷의 status
                .andExpect(jsonPath("$.data.userId").value(1));    // data 안의 userId
    }

    @Test
    @DisplayName("아이디가 중복이면 409를 돌려준다")
    void signUp_duplicateLoginId() throws Exception {
        // given: 서비스가 중복 예외를 던진다고 정함
        given(authService.signUp(any(CreateUserRequest.class)))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict());             // 409
    }

    @Test
    @DisplayName("아이디에 대문자가 있으면 400을 돌려주고 서비스를 부르지 않는다")
    void signUp_invalidLoginId() throws Exception {
        String body = VALID_BODY.replace("traveler01", "Traveler01");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());           // 400

        // @Valid 에서 막혔으니 서비스는 한 번도 호출되면 안 된다
        verify(authService, never()).signUp(any());
    }

    @Test
    @DisplayName("비밀번호에 숫자가 없으면 400을 돌려준다")
    void signUp_invalidPassword() throws Exception {
        String body = VALID_BODY.replace("pass1234word", "passwordonly");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(authService, never()).signUp(any());
    }

    @Test
    @DisplayName("생년월일이 없으면 400을 돌려준다")
    void signUp_missingBirthDate() throws Exception {
        String body = VALID_BODY.replace("\"birthDate\": \"1998-05-17\",", "");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(authService, never()).signUp(any());
    }
}