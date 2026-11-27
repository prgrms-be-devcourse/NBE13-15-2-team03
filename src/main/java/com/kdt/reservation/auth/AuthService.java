package com.kdt.reservation.auth;

import com.kdt.reservation.auth.dto.*;
import com.kdt.reservation.auth.jwt.JwtProvider;
import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.user.Role;
import com.kdt.reservation.user.UserRepository;
import com.kdt.reservation.user.Users;
import io.jsonwebtoken.Claims;
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
     * 로그인 (api-spec 3.2)
     * 아이디·비밀번호 어느 쪽이 틀렸는지 구분하지 않는다 (가입된 아이디가 새어 나가지 않게)
     * refresh 토큰은 컨트롤러가 쿠키에 담을 수 있게 LoginResult 에 따로 담아 돌려준다
     */
    public LoginResult login(LoginRequest request) {
        // 1. 아이디로 회원 찾기. 없거나 탈퇴한 회원이면 실패
        Users user = userRepository.findByLoginId(request.loginId())
                .filter(u -> !u.isDeleted())     // 탈퇴 회원 걸러내기
                .orElseThrow(this::loginFailed);

        // 2. 비밀번호 비교. BCrypt 는 matches(평문, 해시) 로 비교한다
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw loginFailed();
        }

        // 3. 토큰 발급 + refresh 저장
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        // 4. refresh 토큰 저장 (재발급, 로그아웃에서 확인하려고)
        refreshTokenStore.save(user.getId(), refreshToken);

        LoginResponse response = new LoginResponse(
                "Bearer",
                accessToken,
                jwtProvider.getAccessSeconds(),
                new LoginResponse.UserInfo(user.getId(), user.getNickname(), user.getRole()));

        return new LoginResult(response, refreshToken);
    }


    /**
     * 토큰 재발급 (api-spec 3.3)
     * 새 access + 새 refresh 를 발급하고, 기존 refresh 는 즉시 폐기한다 (토큰 회전)
     * 실패는 전부 INVALID_REFRESH_TOKEN (401)
     *
     * @param refreshToken 쿠키에서 꺼낸 값. 쿠키가 없으면 null
     */
    public ReissueResult reissue(String refreshToken) {
        // 1. 쿠키가 없거나 비어 있음
        if (refreshToken == null || refreshToken.isBlank()) {
            throw invalidRefreshToken();
        }

        // 2. 서명·만료·종류(refresh 인지) 검사 → 회원 번호 꺼내기
        Long userId;
        try {
            Claims claims = jwtProvider.parseRefreshToken(refreshToken);
            userId = jwtProvider.getUserId(claims);
        } catch (TokenFailedException e) {
            throw invalidRefreshToken();
        }

        // 3. 서버에 저장된 토큰과 같은지
        //    다르면: 로그아웃했거나, 이미 재발급에 한 번 쓴(폐기된) 토큰
        if (!refreshTokenStore.matches(userId, refreshToken)) {
            throw invalidRefreshToken();
        }

        // 4. 회원 확인 (그사이 탈퇴했을 수 있음). 역할도 DB 의 최신 값으로 다시 읽는다
        Users user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(this::invalidRefreshToken);

        // 5. 새로 발급하고 저장소를 새 refresh 로 교체 → 기존 refresh 는 이 순간부터 무효
        String newAccessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        String newRefreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenStore.save(user.getId(), newRefreshToken);

        ReissueTokenResponse response = new ReissueTokenResponse(
                "Bearer", newAccessToken, jwtProvider.getAccessSeconds());

        return new ReissueResult(response, newRefreshToken);
    }


    /**
     * 로그아웃 (api-spec 3.4)
     * 저장소의 refresh 를 지운다 → 이후 재발급 불가.
     * 이미 발급된 access 토큰은 만료 시각(최대 30분)까지는 유효하다 (명세대로)
     */
    public void logout(Long userId) {
        refreshTokenStore.delete(userId);
    }

    // 로그인 실패 예외. 두 군데서 써서 메서드로 뺐다
    private BusinessException loginFailed() {
        return new BusinessException(ErrorCode.LOGIN_FAILED);
    }

    private BusinessException invalidRefreshToken() {
        return new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
    }
}
