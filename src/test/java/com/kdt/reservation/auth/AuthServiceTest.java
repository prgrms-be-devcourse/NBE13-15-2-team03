package com.kdt.reservation.auth;

import com.kdt.reservation.auth.dto.CreateUserRequest;
import com.kdt.reservation.auth.jwt.JwtProvider;
import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.point.PointService;
import com.kdt.reservation.user.Role;
import com.kdt.reservation.user.UserRepository;
import com.kdt.reservation.user.Users;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// AuthService 테스트 (회원가입 시 기본 포인트 생성)
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private PointService pointService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("가입에 성공하면 저장한 회원의 기본 포인트를 만든다")
    void signUp_createsDefaultPoint() {
        Users saved = Users.createLocal("traveler01", "encoded", "김여행", "여행자",
                "01012345678", LocalDate.of(1998, 5, 17), Role.GUEST);
        given(passwordEncoder.encode("pass1234word")).willReturn("encoded");
        given(userRepository.save(any(Users.class))).willReturn(saved);

        authService.signUp(request(Role.GUEST));

        verify(pointService).createDefault(saved);
    }

    @Test
    @DisplayName("아이디가 중복이면 기본 포인트를 만들지 않는다")
    void signUp_duplicateLoginId() {
        given(userRepository.existsByLoginId("traveler01")).willReturn(true);

        assertThatThrownBy(() -> authService.signUp(request(Role.GUEST)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_LOGIN_ID);

        verify(pointService, never()).createDefault(any());
    }

    @Test
    @DisplayName("전화번호가 중복이면 기본 포인트를 만들지 않는다")
    void signUp_duplicatePhone() {
        given(userRepository.existsByPhone("01012345678")).willReturn(true);

        assertThatThrownBy(() -> authService.signUp(request(Role.GUEST)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_PHONE);

        verify(pointService, never()).createDefault(any());
    }

    @Test
    @DisplayName("ADMIN으로 가입하면 기본 포인트를 만들지 않는다")
    void signUp_adminRole() {
        assertThatThrownBy(() -> authService.signUp(request(Role.ADMIN)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

        verify(pointService, never()).createDefault(any());
    }

    // 역할만 바꾼 가입 요청을 만든다
    private CreateUserRequest request(Role role) {
        return new CreateUserRequest("traveler01", "pass1234word", "김여행", "여행자",
                "01012345678", LocalDate.of(1998, 5, 17), role);
    }
}