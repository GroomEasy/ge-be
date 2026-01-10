package com.ceos.menual.global.config.redis;

import java.time.Duration;

public interface RefreshTokenStore {
    void save(Long userId, String refreshToken, Duration ttl);
    String get(Long userId);
    void delete(Long userId);
}