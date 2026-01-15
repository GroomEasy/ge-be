package com.ceos.menual.domain.expert.service.zoom;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class RedisZoomOAuthStateStore implements ZoomOAuthStateStore {

    private static final String PREFIX = "zoom:oAuthState:";

    private final StringRedisTemplate redis;

    @Override
    public void save(String state, Long userId, Duration ttl) {
        redis.opsForValue().set(PREFIX + state, String.valueOf(userId), ttl);
    }

    @Override
    public Optional<Long> consume(String state) {
        String key = PREFIX + state;
        String val = redis.opsForValue().getAndDelete(key);
        if (val == null) return Optional.empty();
        try {
            return Optional.of(Long.parseLong(val));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}

