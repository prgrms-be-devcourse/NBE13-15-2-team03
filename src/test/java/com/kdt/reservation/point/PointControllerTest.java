package com.kdt.reservation.point;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// PointController 테스트 (포인트 잔액 조회)
@WebMvcTest(PointController.class)
@AutoConfigureMockMvc(addFilters = false)
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PointService pointService;

    private static final UsernamePasswordAuthenticationToken LOGIN_USER_1 =
            new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_GUEST")));

    @BeforeEach
    void setUpLogin() {
        SecurityContextHolder.getContext().setAuthentication(LOGIN_USER_1);
    }

    @AfterEach
    void clearLogin() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("포인트 잔액 조회 성공 시 200과 잔액을 돌려준다")
    void getMyPoint_success() throws Exception {
        given(pointService.getMyPoint(1L)).willReturn(new PointResponse(23750));

        mockMvc.perform(get("/api/points/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.balance").value(23750));
    }

    @Test
    @DisplayName("토큰 속 회원이 없거나 탈퇴했으면 401을 돌려준다")
    void getMyPoint_userNotFound() throws Exception {
        given(pointService.getMyPoint(1L)).willThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        mockMvc.perform(get("/api/points/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("포인트 행이 없으면 500 POINT_NOT_FOUND를 돌려준다")
    void getMyPoint_pointNotFound() throws Exception {
        given(pointService.getMyPoint(1L)).willThrow(new BusinessException(ErrorCode.POINT_NOT_FOUND));

        mockMvc.perform(get("/api/points/me"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("POINT_NOT_FOUND"));
    }
}
