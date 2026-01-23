package com.ceos.menual.global.config.redis;

/**
 * 분산 락 서비스 인터페이스
 * 여러 인스턴스에서 동시에 실행되는 스케줄러의 중복 실행을 방지
 */
public interface DistributedLockService {

    /**
     * 락 획득 시도
     *
     * @param lockKey 락 키
     * @param ttlSeconds 락 만료 시간 (초)
     * @return 락 획득 성공 여부
     */
    boolean tryLock(String lockKey, long ttlSeconds);

    /**
     * 락 해제
     *
     * @param lockKey 락 키
     */
    void unlock(String lockKey);
}
