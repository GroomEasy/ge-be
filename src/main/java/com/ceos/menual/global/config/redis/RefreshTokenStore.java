package com.ceos.menual.global.config.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private final StringRedisTemplate redis;
    private static final String PREFIX = "refresh:"; // refresh:{userId}

    public void save(Long userId, String refreshToken, Duration ttl) {
        redis.opsForValue().set(PREFIX + userId, refreshToken, ttl);
    }

    public String get(Long userId) {
        return redis.opsForValue().get(PREFIX + userId);
    }

    public void delete(Long userId) {
        redis.delete(PREFIX + userId);
    }
}