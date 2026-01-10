package com.ceos.menual.global.config.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private final StringRedisTemplate redis;
    private static final String PREFIX = "refresh:";

    @Override
    public void save(Long userId, String refreshToken, Duration ttl) {
        String key = PREFIX + userId;
        redis.opsForValue().set(key, refreshToken, ttl);
        log.debug("RefreshToken saved. userId={}, ttl={}", userId, ttl);
    }

    @Override
    public String get(Long userId) {
        String key = PREFIX + userId;
        return redis.opsForValue().get(key);
    }

    @Override
    public void delete(Long userId) {
        String key = PREFIX + userId;
        redis.delete(key);
        log.debug("RefreshToken deleted. userId={}", userId);
    }
}