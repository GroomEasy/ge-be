package com.ceos.menual.domain.expert.service.zoom;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile({"local", "test"})
public class InMemoryZoomOAuthStateStore implements ZoomOAuthStateStore {

    private static final class Entry {
        private final Long userId;
        private final long expiresAtMillis;

        private Entry(Long userId, long expiresAtMillis) {
            this.userId = userId;
            this.expiresAtMillis = expiresAtMillis;
        }
    }

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void save(String state, Long userId, Duration ttl) {
        long expiresAt = System.currentTimeMillis() + ttl.toMillis();
        store.put(state, new Entry(userId, expiresAt));
    }

    @Override
    public Optional<Long> consume(String state) {
        Entry entry = store.remove(state);
        if (entry == null) return Optional.empty();
        if (System.currentTimeMillis() > entry.expiresAtMillis) return Optional.empty();
        return Optional.of(entry.userId);
    }
}

