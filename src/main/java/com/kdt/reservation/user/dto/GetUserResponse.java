package com.kdt.reservation.user.dto;

import com.kdt.reservation.user.Role;
import com.kdt.reservation.user.Users;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 내 정보 조회 응답 (api-spec 5.1)
 *
 * 엔티티(Users)를 그대로 응답하지 않는 이유: password(해시)까지 밖으로 나가기 때문.
 * 응답에 필요한 값만 골라 담는다.
 */
public record GetUserResponse(
        Long userId,
        String loginId,
        String name,
        String nickname,
        String phone,                  // TODO 소셜 가입 임시 값이면 null (소셜 로그인 작업 때)
        LocalDate birthDate,           // TODO 소셜 가입 임시 값(1900-01-01)이면 null (소셜 로그인 작업 때)
        Role role,
        List<String> socialProviders,  // 연결된 소셜 계정. 없으면 빈 배열
        LocalDateTime createdAt
) {

    // 엔티티 + 소셜 목록 → 응답 객체로 바꾸는 메서드. 바꾸는 규칙을 한 곳에
    public static GetUserResponse of(Users user, List<String> socialProviders) {
        return new GetUserResponse(
                user.getId(),
                user.getLogin_id(),
                user.getName(),
                user.getNickname(),
                user.getPhone(),
                user.getBirth_date(),
                user.getRole(),
                socialProviders,
                user.getCreated_at());
    }
}