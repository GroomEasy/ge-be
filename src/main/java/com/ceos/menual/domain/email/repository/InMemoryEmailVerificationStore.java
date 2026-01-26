package com.ceos.menual.domain.email.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@Profile({"local", "test"})
public class InMemoryEmailVerificationStore implements EmailVerificationStore {

    private final Map<String, ExpiringValue> codeStore = new ConcurrentHashMap<>();
    private final Map<String, ExpiringValue> verifiedStore = new ConcurrentHashMap<>();

    private static final String CODE_PREFIX = "email:verify:";
    private static final String VERIFIED_PREFIX = "email:verified:";

    @Override
    public void saveCode(String email, String code, Duration ttl) {
        String key = CODE_PREFIX + email;
        Instant expiresAt = Instant.now().plus(ttl);
        codeStore.put(key, new ExpiringValue(code, expiresAt));
        log.debug("Verification code saved (in-memory). email={}, ttl={}", email, ttl);
    }

    @Override
    public String getCode(String email) {
        String key = CODE_PREFIX + email;
        ExpiringValue value = codeStore.get(key);

        if (value == null) {
            return null;
        }

        if (value.isExpired()) {
            codeStore.remove(key);
            return null;
        }

        return value.getValue();
    }

    @Override
    public void deleteCode(String email) {
        String key = CODE_PREFIX + email;
        codeStore.remove(key);
        log.debug("Verification code deleted (in-memory). email={}", email);
    }

    @Override
    public void saveVerified(String email, Duration ttl) {
        String key = VERIFIED_PREFIX + email;
        Instant expiresAt = Instant.now().plus(ttl);
        verifiedStore.put(key, new ExpiringValue("true", expiresAt));
        log.debug("Email verified (in-memory). email={}, ttl={}", email, ttl);
    }

    @Override
    public boolean isVerified(String email) {
        String key = VERIFIED_PREFIX + email;
        ExpiringValue value = verifiedStore.get(key);

        if (value == null) {
            return false;
        }

        if (value.isExpired()) {
            verifiedStore.remove(key);
            return false;
        }

        return true;
    }

    @Override
    public void deleteVerified(String email) {
        String key = VERIFIED_PREFIX + email;
        verifiedStore.remove(key);
        log.debug("Verified status deleted (in-memory). email={}", email);
    }

    private record ExpiringValue(String value, Instant expiresAt) {
        public String getValue() {
            return value;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}