package com.kdt.reservation.point;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// PointController 테스트 (포인트 잔액 조회, 포인트 내역 조회)
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

    @Test
    @DisplayName("포인트 내역 조회 성공 시 200과 페이지 응답을 돌려준다")
    void getMyPointHistory_success() throws Exception {
        Page<PointHistoryResponse> page = new PageImpl<>(
                List.of(
                        new PointHistoryResponse(302L, PointType.EARN, 13750, 501L,
                                LocalDateTime.of(2026, 12, 1, 15, 3, 7)),
                        new PointHistoryResponse(301L, PointType.USE, 5000, null,
                                LocalDateTime.of(2026, 12, 1, 15, 3, 7))),
                PageRequest.of(0, 20), 2);
        given(pointService.getMyPointHistory(eq(1L), any(), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/api/points/me/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].pointHistoryId").value(302))
                .andExpect(jsonPath("$.data.content[0].type").value("EARN"))
                .andExpect(jsonPath("$.data.content[0].amount").value(13750))
                .andExpect(jsonPath("$.data.content[0].reservationId").value(501))
                .andExpect(jsonPath("$.data.content[0].createdAt").value("2026-12-01T15:03:07"))
                .andExpect(jsonPath("$.data.content[1].reservationId").value(nullValue()))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));

        verify(pointService).getMyPointHistory(eq(1L), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("type을 보내면 해당 유형으로 서비스를 호출한다")
    void getMyPointHistory_typeFilter() throws Exception {
        given(pointService.getMyPointHistory(eq(1L), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/points/me/history").param("type", "USE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));

        verify(pointService).getMyPointHistory(eq(1L), eq(PointType.USE), any(Pageable.class));
    }

    @Test
    @DisplayName("정의되지 않은 type이면 400 INVALID_INPUT을 돌려준다")
    void getMyPointHistory_invalidType() throws Exception {
        mockMvc.perform(get("/api/points/me/history").param("type", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("소문자 type이면 400 INVALID_INPUT을 돌려준다")
    void getMyPointHistory_lowercaseType() throws Exception {
        mockMvc.perform(get("/api/points/me/history").param("type", "earn"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("내역 조회 시 토큰 속 회원이 없거나 탈퇴했으면 401을 돌려준다")
    void getMyPointHistory_userNotFound() throws Exception {
        given(pointService.getMyPointHistory(eq(1L), any(), any(Pageable.class)))
                .willThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        mockMvc.perform(get("/api/points/me/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("내역 조회 시 포인트 행이 없으면 500 POINT_NOT_FOUND를 돌려준다")
    void getMyPointHistory_pointNotFound() throws Exception {
        given(pointService.getMyPointHistory(eq(1L), any(), any(Pageable.class)))
                .willThrow(new BusinessException(ErrorCode.POINT_NOT_FOUND));

        mockMvc.perform(get("/api/points/me/history"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("POINT_NOT_FOUND"));
    }
}
