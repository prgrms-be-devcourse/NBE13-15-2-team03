package com.kdt.reservation.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

// 명세 1.2절의 에러 응답 본문
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        int status,
        String error,
        String message,
        List<FieldError> errors,
        LocalDateTime timestamp
) {

    // 입력값 검증에 실패한 필드와 그 이유
    public record FieldError(String field, String reason) {
    }

    // 필드 오류가 없는 에러 응답을 만든다
    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, List.of());
    }

    // 필드 오류 목록을 담은 에러 응답을 만든다
    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors) {
        return new ErrorResponse(
                errorCode.getStatus().value(),
                errorCode.name(),
                errorCode.getMessage(),
                errors,
                LocalDateTime.now()
        );
    }
}
