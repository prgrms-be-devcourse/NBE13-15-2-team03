package com.kdt.reservation.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Long> {

    // 필드명에 _ 가 있으면 메서드 이름에서는 __ 두 개로 쓴다 (login_id → Login__id)
    // 탈퇴 회원도 포함해서 검사. 탈퇴한 아이디는 재사용 불가 (api-spec 0-1 18번)
    //boolean existsByLogin__id(String loginId);
    @Query("select count(u) > 0 from Users u where u.login_id = :loginId")
    boolean existsByLoginId(@Param("loginId") String loginId);//

    boolean existsByPhone(String phone);

    // 로그인용: 아이디로 회원 찾기. 없을 수도 있어서 Optional
    @Query("select u from Users u where u.login_id = :loginId")
    Optional<Users> findByLoginId(@Param("loginId") String loginId);
}
