package com.kdt.reservation.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh 토큰입니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),

    DUPLICATE_PHONE(HttpStatus.CONFLICT, "이미 사용 중인 전화번호입니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),
    PASSWORD_NOT_SET(HttpStatus.BAD_REQUEST, "비밀번호가 설정되지 않은 소셜 로그인 계정입니다."),
    USER_NOT_FOUND(HttpStatus.UNAUTHORIZED,"사용자를 찾을 수 없습니다"),
    USER_HAS_ACTIVE_RESERVATION(HttpStatus.CONFLICT, "체크아웃 전인 예약이 있어 탈퇴할 수 없습니다."),

    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "날짜 범위가 올바르지 않습니다."),
    INVALID_PRICE_RANGE(HttpStatus.BAD_REQUEST, "가격 범위가 올바르지 않습니다."),
    NOT_ACCOMMODATION_OWNER(HttpStatus.FORBIDDEN, "본인의 숙소가 아닙니다."),
    ACCOMMODATION_NOT_FOUND(HttpStatus.NOT_FOUND, "숙소를 찾을 수 없습니다."),
    ACCOMMODATION_HAS_ACTIVE_RESERVATION(HttpStatus.CONFLICT, "체크아웃 전인 예약이 있어 숙소를 삭제할 수 없습니다."),
    INVALID_FEATURE(HttpStatus.BAD_REQUEST, "편의시설이 올바르지 않습니다."),

    INVALID_CAPACITY(HttpStatus.BAD_REQUEST, "최대 인원은 기준 인원보다 작을 수 없습니다."),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "객실을 찾을 수 없습니다."),
    ROOM_HAS_ACTIVE_RESERVATION(HttpStatus.CONFLICT, "체크아웃 전인 예약이 있어 객실을 삭제할 수 없습니다."),

    GUEST_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "객실 최대 인원을 초과했습니다."),
    RESERVATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "예약에 접근할 권한이 없습니다."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."),
    ROOM_NOT_AVAILABLE(HttpStatus.CONFLICT, "판매 중지된 객실입니다."),
    RESERVATION_DATE_CONFLICT(HttpStatus.CONFLICT, "해당 날짜에 이미 예약이 있습니다."),
    RESERVATION_ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 예약입니다."),
    CANCEL_PERIOD_EXPIRED(HttpStatus.CONFLICT, "취소 가능 기간이 지났습니다."),

    INVALID_POINT_AMOUNT(HttpStatus.BAD_REQUEST, "사용 포인트가 올바르지 않습니다."),
    RESERVATION_NOT_PAYABLE(HttpStatus.CONFLICT, "결제할 수 없는 예약입니다."),
    INSUFFICIENT_POINT(HttpStatus.CONFLICT, "보유 포인트가 부족합니다."),

    REVIEW_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "리뷰를 작성할 수 없는 예약입니다."),
    NOT_REVIEW_AUTHOR(HttpStatus.FORBIDDEN, "본인의 리뷰가 아닙니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 리뷰를 작성한 예약입니다."),

    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "허용되지 않은 파일 형식입니다."),
    FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "파일 크기는 5MB를 넘을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
