package com.ceos.menual.domain.email.repository;

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
public class RedisEmailVerificationStore implements EmailVerificationStore {

    private final StringRedisTemplate redis;

    private static final String CODE_PREFIX = "email:verify:";
    private static final String VERIFIED_PREFIX = "email:verified:";

    @Override
    public void saveCode(String email, String code, Duration ttl) {
        String key = CODE_PREFIX + email;
        redis.opsForValue().set(key, code, ttl);
        log.debug("Verification code saved. email={}, ttl={}", email, ttl);
    }

    @Override
    public String getCode(String email) {
        String key = CODE_PREFIX + email;
        return redis.opsForValue().get(key);
    }

    @Override
    public void deleteCode(String email) {
        String key = CODE_PREFIX + email;
        redis.delete(key);
        log.debug("Verification code deleted. email={}", email);
    }

    @Override
    public void saveVerified(String email, Duration ttl) {
        String key = VERIFIED_PREFIX + email;
        redis.opsForValue().set(key, "true", ttl);
        log.debug("Email verified. email={}, ttl={}", email, ttl);
    }

    @Override
    public boolean isVerified(String email) {
        String key = VERIFIED_PREFIX + email;
        return redis.hasKey(key);
    }

    @Override
    public void deleteVerified(String email) {
        String key = VERIFIED_PREFIX + email;
        redis.delete(key);
        log.debug("Verified status deleted. email={}", email);
    }
}