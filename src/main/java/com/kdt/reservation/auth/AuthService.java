package com.kdt.reservation.auth;

import com.kdt.reservation.auth.dto.CreateUserRequest;
import com.kdt.reservation.auth.dto.CreateUserResponse;
import com.kdt.reservation.auth.dto.LoginRequest;
import com.kdt.reservation.auth.dto.LoginResponse;
import com.kdt.reservation.auth.jwt.JwtProvider;
import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.user.Role;
import com.kdt.reservation.user.UserRepository;
import com.kdt.reservation.user.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional
    public CreateUserResponse signUp(CreateUserRequest request){
        if (request.role() == Role.ADMIN){
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        if (userRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE);
        }

        //  비밀번호 암호화 후 회원 객체 만들기
        Users user = Users.createLocal(
                request.loginId(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.nickname(),
                request.phone(),
                request.birthDate(),
                request.role());

        Users saved = userRepository.save(user);

        return new CreateUserResponse(saved.getId());
    }

    /**
     * 로그인 (api-spec)
     * 아이디가 틀렸는지 비밀번호가 틀렸는지 구분해서 알려 주지 않는다.
     * 구분해 주면 "이 아이디는 가입돼 있다"는 정보가 새어 나간다
     */
    public LoginResponse login(LoginRequest request) {
        // 1. 아이디로 회원 찾기. 없거나 탈퇴한 회원이면 실패
        Users user = userRepository.findByLoginId(request.loginId())
                .filter(u -> !u.isDeleted())     // 탈퇴 회원 걸러내기
                .orElseThrow(this::loginFailed);

        // 2. 비밀번호 비교. BCrypt 는 matches(평문, 해시) 로 비교한다
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw loginFailed();
        }

        // 3. 토큰 발급
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        // 4. refresh 토큰 저장 (재발급, 로그아웃에서 확인하려고)
        refreshTokenStore.save(user.getId(), refreshToken);

        return new LoginResponse(
                "Bearer",
                accessToken,
                jwtProvider.getAccessSeconds(),
                refreshToken,
                new LoginResponse.UserInfo(user.getId(), user.getNickname(), user.getRole()));
    }

    // 로그인 실패 예외. 두 군데서 써서 메서드로 뺐다
    private BusinessException loginFailed() {
        return new BusinessException(ErrorCode.LOGIN_FAILED);
    }

}
