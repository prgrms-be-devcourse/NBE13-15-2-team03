package com.kdt.reservation.auth.security;

import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 401: 토큰 없음, 만료, 위조
 * 필터 단계의 에러는 GlobalExceptionHandler 가 못 잡아서 여기서 직접 응답을 쓴다.
 * 형식은 팀 공통 ErrorResponse 를 그대로 써서 다른 에러와 모양을 맞춘다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // 객체를 JSON 문자열로 바꿔 주는 도구 (스프링이 만들어 둔 것을 주입받음)
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode));
    }
}
