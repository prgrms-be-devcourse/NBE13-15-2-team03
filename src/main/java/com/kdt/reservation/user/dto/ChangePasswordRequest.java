package com.kdt.reservation.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 변경 요청 (api-spec 5.3)
 *
 * 현재 비밀번호를 같이 받는 이유: 로그인된 기기를 다른 사람이 잠깐 만져도
 * 비밀번호를 바꿔서 계정을 뺏지 못하게 하려고.
 */
public record ChangePasswordRequest(

        @NotBlank
        String currentPassword,

        // 회원가입과 같은 규칙: 8~64자, 영문과 숫자 모두 포함
        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "비밀번호는 영문과 숫자를 모두 포함해야 합니다.")
        String newPassword
) {
}