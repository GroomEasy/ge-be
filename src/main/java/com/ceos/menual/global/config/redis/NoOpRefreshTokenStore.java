package com.ceos.menual.global.config.redis;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Profile({"local", "test"})
public class NoOpRefreshTokenStore implements RefreshTokenStore {
    @Override public void save(Long userId, String refreshToken, Duration ttl) {}
    @Override public String get(Long userId) { return null; }
    @Override public void delete(Long userId) {}
}