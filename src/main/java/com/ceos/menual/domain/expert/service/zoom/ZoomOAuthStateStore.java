package com.ceos.menual.domain.expert.service.zoom;

import java.time.Duration;
import java.util.Optional;

public interface ZoomOAuthStateStore {
    void save(String state, Long userId, Duration ttl);

    /**
     * 1회성 사용을 위해 조회와 동시에 삭제
     */
    Optional<Long> consume(String state);
}

