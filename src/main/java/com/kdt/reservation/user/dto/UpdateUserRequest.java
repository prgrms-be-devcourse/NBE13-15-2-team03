package com.kdt.reservation.user.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 내 정보 수정 요청 (api-spec 5.2)
 *
 * 모든 필드가 선택이다. 보내지 않은 필드는 null 로 들어오고, null 은 "수정 안 함"이다.
 * 검증 어노테이션(@Size 등)은 값이 null 이면 검사하지 않고 통과한다 → 보낸 필드만 검사된다.
 * "1개 이상은 보내야 한다"는 규칙은 서비스에서 검사한다.
 */
public record UpdateUserRequest(

        @Size(min = 1, max = 50)
        String name,

        @Size(min = 2, max = 20)
        String nickname,

        @Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 숫자만 10~11자리여야 합니다.")
        String phone,

        @Past
        LocalDate birthDate
) {

    // 보낸 필드가 하나도 없는지
    public boolean isEmpty() {
        return name == null && nickname == null && phone == null && birthDate == null;
    }
}