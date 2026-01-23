package com.ceos.menual.global.config.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis 기반 분산 락 서비스 구현체
 * 프로덕션 환경에서 사용
 */
@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class RedisDistributedLockService implements DistributedLockService {

    private final StringRedisTemplate stringRedisTemplate;

    // 현재 인스턴스의 고유 식별자 (락 소유권 확인용)
    private final String instanceId = UUID.randomUUID().toString();

    // 락 키 프리픽스
    private static final String LOCK_PREFIX = "scheduler:lock:";

    @Override
    public boolean tryLock(String lockKey, long ttlSeconds) {
        String fullKey = LOCK_PREFIX + lockKey;
        String lockValue = instanceId + ":" + System.currentTimeMillis();

        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(fullKey, lockValue, Duration.ofSeconds(ttlSeconds));

            if (Boolean.TRUE.equals(acquired)) {
                log.debug("분산 락 획득 성공 - key: {}, instanceId: {}", lockKey, instanceId);
                return true;
            } else {
                log.debug("분산 락 획득 실패 (다른 인스턴스가 보유 중) - key: {}", lockKey);
                return false;
            }
        } catch (Exception e) {
            log.error("분산 락 획득 중 오류 발생 - key: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public void unlock(String lockKey) {
        String fullKey = LOCK_PREFIX + lockKey;

        try {
            stringRedisTemplate.delete(fullKey);
            log.debug("분산 락 해제 완료 - key: {}", lockKey);
        } catch (Exception e) {
            log.error("분산 락 해제 중 오류 발생 - key: {}", lockKey, e);
        }
    }
}
