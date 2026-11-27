package com.kdt.reservation.user;

import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.user.dto.GetUserResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

    /**
     * 내 정보 조회 (api-spec 5.1)
     */
    public GetUserResponse getMe(Long userId) {
        Users user = findActiveUser(userId);
        List<String> socialProviders = socialUserRepository.findProvidersByUserId(userId);
        return GetUserResponse.of(user, socialProviders);
    }

    /**
     * 토큰 속 회원을 찾는다. 없거나 탈퇴했으면 401.
     *
     * 404 가 아니라 401 인 이유: "요청한 사람"의 신원이 무효한 것이라 인증 문제다.
     * 프론트는 401 을 받으면 로그인 화면으로 보낸다.
     *
     * 정보 수정, 비밀번호 변경에서도 똑같이 쓰려고 메서드로 뺐다.
     */
    public Users findActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> !user.isDeleted())   // 탈퇴 회원 걸러내기
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}