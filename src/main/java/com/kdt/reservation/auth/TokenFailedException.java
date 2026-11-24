package com.kdt.reservation.auth;

public class TokenFailedException extends RuntimeException {
    public TokenFailedException() {
        super("유효하지 않은 토큰입니다.");
    }
}
