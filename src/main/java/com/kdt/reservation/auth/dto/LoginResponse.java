package com.kdt.reservation.auth.dto;

import com.kdt.reservation.user.Role;

/**
 * 로그인 응답 (api-spec)
 *
 * {
 *   "tokenType": "Bearer",
 *   "accessToken": "eyJ...",
 *   "accessTokenExpiresIn": 1800,
 *   "refreshToken": "eyJ...",
 *   "user": { "userId": 7, "nickname": "여행자", "role": "GUEST" }
 * }
 */
public record LoginResponse(
        String tokenType,              // 항상 "Bearer". 프론트가 헤더에 "Bearer 토큰" 으로 붙이라는 뜻
        String accessToken,
        long accessTokenExpiresIn,     // access 토큰 남은 시간(초)
        String refreshToken,
        UserInfo user
) {

    // 응답 안의 "user": { ... } 부분. record 안에 record 를 넣을 수 있다
    /*
    * {
  "tokenType": "Bearer",
  "accessToken": "eyJ...",
  "accessTokenExpiresIn": 1800,
  "refreshToken": "eyJ...",
  "user": {                      ← 여기가 객체 안의 객체 (api-spec)
    "userId": 7,
    "nickname": "여행자",
    "role": "GUEST"
  }
}
    * */
    public record UserInfo(Long userId, String nickname, Role role) {
    }
}