package com.kdt.reservation.user;

import com.kdt.reservation.auth.RefreshTokenStore;
import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.user.dto.ChangePasswordRequest;
import com.kdt.reservation.user.dto.GetUserResponse;
import java.util.List;

import com.kdt.reservation.user.dto.UpdateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 기능 (/api/users/me)
 *
 * userId 는 컨트롤러에서 @AuthenticationPrincipal 로 받은 "토큰 속 회원 번호".
 * (JwtAuthenticationFilter 가 보관함에 넣어 둔 값)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 기본은 읽기 전용. 수정 메서드(정보 수정, 비밀번호 변경)는 따로 @Transactional
public class UserService {

    private final UserRepository userRepository;
    private final SocialUserRepository socialUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenStore refreshTokenStore;

    //내 정보 조회 (api-spec 5.1)
    public GetUserResponse getMe(Long userId) {
        Users user = findActiveUser(userId);
        return toResponse(user);
    }

    /**
     * 내 정보 수정 (api-spec 5.2)
     *
     * save() 를 부르지 않는다. @Transactional 안에서 엔티티 값을 바꾸면
     * 트랜잭션이 끝날 때 JPA 가 바뀐 걸 감지해서 UPDATE 를 알아서 보낸다 (변경 감지, dirty checking).
     */
    @Transactional
    public GetUserResponse updateMe(Long userId, UpdateUserRequest request) {
        // 1. 보낸 필드가 하나도 없으면 400 (명세: 1개 이상 필요)
        if (request.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        Users user = findActiveUser(userId);

        // 2. 전화번호를 "다른 번호로" 바꿀 때만 중복 검사
        //    지금 내 번호와 같은 걸 보내면 검사하지 않는다 (내 번호가 이미 있으니 무조건 중복으로 나와서)
        String newPhone = request.phone();
        if (newPhone != null && !newPhone.equals(user.getPhone()) && userRepository.existsByPhone(newPhone)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE);
        }

        // 3. 보낸 값만 바꾼다 (null 은 엔티티 메서드 안에서 건너뜀)
        user.updateProfile(request.name(), request.nickname(), newPhone, request.birthDate());

        return toResponse(user);
    }

    /**
     * 비밀번호 변경 (api-spec 5.3)
     * 성공하면 저장된 refresh 를 지워서 재발급을 막는다 → access 가 만료되면 다시 로그인해야 한다
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        Users user = findActiveUser(userId);

        // 1. 소셜 전용 계정은 비밀번호가 서버가 만든 임시 값이라 바꿀 수 없다
        if (isSocialOnly(user)) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_SET);
        }

        // 2. 현재 비밀번호 확인. 해시끼리 equals 가 아니라 matches(평문, 해시)로 비교
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 3. 새 비밀번호를 암호화해서 저장 (변경 감지로 UPDATE)
        user.changePassword(passwordEncoder.encode(request.newPassword()));

        // 4. refresh 삭제 → 다른 기기 포함 재발급 불가
        refreshTokenStore.delete(userId);
    }

    /**
     * 토큰 속 회원을 찾는다. 없거나 탈퇴했으면 401.
     * 404 가 아니라 401 인 이유: "요청한 사람"의 신원이 무효한 것이라 인증 문제다.
     * 프론트는 401 을 받으면 로그인 화면으로 보낸다.
     * 정보 수정, 비밀번호 변경에서도 똑같이 쓰려고 메서드로 뺐다.
     */
    public Users findActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> !user.isDeleted())   // 탈퇴 회원 걸러내기
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    /**
     * 소셜 전용 계정인지 (api-spec 0-1 4번 기준)
     * 1) 소셜 연결이 있고
     * 2) 아이디가 서버가 만든 형식({provider}_{번호}, 예: kakao_123456)
     *
     * 일반 회원가입 아이디는 영문 소문자·숫자만 허용해서 "_" 가 들어갈 수 없다.
     * 그래서 아이디에 "_" 가 있으면 서버가 만든 아이디다.
     */
    private boolean isSocialOnly(Users user) {
        return user.getLogin_id().contains("_") && socialUserRepository.existsByUserId(user.getId());
    }

    private GetUserResponse toResponse(Users user) {
        List<String> socialProviders = socialUserRepository.findProvidersByUserId(user.getId());
        return GetUserResponse.of(user, socialProviders);
    }
}