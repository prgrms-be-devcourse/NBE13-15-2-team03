package com.kdt.reservation.auth.dto;

import com.kdt.reservation.user.Role;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateUserRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]{4,20}$", message = "아이디는 영문 소문자와 숫자 4~20자여야 합니다.")
        String loginId,

        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "비밀번호는 영문과 숫자를 모두 포함해야 합니다.")
        String password,

        @NotBlank
        @Size(max = 50)
        String name,

        @NotBlank
        @Size(min = 2, max = 20)
        String nickname,

        // 숫자만 10~11자리 (하이픈 없이)
        @NotBlank
        @Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 숫자만 10~11자리여야 합니다.")
        String phone,

        // 오늘보다 과거. JSON 에서는 "1998-05-17" 형식으로 보낸다
        @NotNull
        @Past
        LocalDate birthDate,

        // GUEST 또는 HOST. ADMIN 은 서비스에서 막는다
        @NotNull
        Role role
) {
}
