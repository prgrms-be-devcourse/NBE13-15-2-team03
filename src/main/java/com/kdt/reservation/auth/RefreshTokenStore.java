package com.kdt.reservation.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Refresh 토큰 저장소 (api-spec 0-1 3번: 서버 인메모리, 사용자당 1개)
 *
 * 왜 저장하는지: 서버가 발급한 refresh 토큰을 기억해 둬야 로그아웃 시 "이 토큰은 이제 무효"로 만들 수 있다
 * ConcurrentHashMap: 여러 요청이 동시에 읽고 써도 안전한 Map
 * 한계: 서버를 재시작하면 전부 사라진다 → 다시 로그인해야 함. 나중에 Redis 로 옮길 예정
 */
@Component
public class RefreshTokenStore {

    // key: 회원 번호, value: 그 회원의 refresh 토큰
    private final Map<Long, String> store = new ConcurrentHashMap<>();

    // 저장. 이미 있으면 덮어쓴다 (사용자당 1개)
    public void save(Long userId, String refreshToken) {
        store.put(userId, refreshToken);
    }

    // 재발급 때 사용: 들고 온 토큰이 저장된 것과 같은지
    public boolean matches(Long userId, String refreshToken) {
        return refreshToken.equals(store.get(userId));
    }

    // 로그아웃 때 사용
    public void delete(Long userId) {
        store.remove(userId);
    }
}