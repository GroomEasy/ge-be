package com.ceos.menual.global.config.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * NoOp 분산 락 서비스 구현체
 * 개발/로컬 환경에서 사용 (단일 인스턴스 환경)
 * 항상 락 획득 성공을 반환
 */
@Slf4j
@Service
@Profile("!prod")
public class NoOpDistributedLockService implements DistributedLockService {

    @Override
    public boolean tryLock(String lockKey, long ttlSeconds) {
        log.debug("NoOp 분산 락 - 락 획득 (단일 인스턴스 모드) - key: {}", lockKey);
        return true;
    }

    @Override
    public void unlock(String lockKey) {
        log.debug("NoOp 분산 락 - 락 해제 (단일 인스턴스 모드) - key: {}", lockKey);
    }
}
