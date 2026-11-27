package com.kdt.reservation.user;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 소셜 계정 연결 정보 Repository
 */
public interface SocialUserRepository extends JpaRepository<SocialUser, Long> {

    // 이 회원에게 연결된 소셜 제공자 목록 (예: ["KAKAO", "GOOGLE"]). 없으면 빈 목록
    // s.user.id : SocialUser 의 user 필드(Users) 안의 id
    @Query("select s.provider from SocialUser s where s.user.id = :userId")
    List<String> findProvidersByUserId(@Param("userId") Long userId);

    // 이 회원에게 연결된 소셜 계정이 하나라도 있는지 (비밀번호 변경 시 소셜 전용 계정 판단용)
    @Query("select count(s) > 0 from SocialUser s where s.user.id = :userId")
    boolean existsByUserId(@Param("userId") Long userId);
}