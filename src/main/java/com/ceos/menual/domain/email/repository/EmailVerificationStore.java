package com.ceos.menual.domain.email.repository;

import java.time.Duration;

public interface EmailVerificationStore {

    /**
     * 인증 코드 저장
     * @param email 이메일 주소
     * @param code 인증 코드
     * @param ttl 유효 시간
     */
    void saveCode(String email, String code, Duration ttl);

    /**
     * 인증 코드 조회
     * @param email 이메일 주소
     * @return 인증 코드 (없으면 null)
     */
    String getCode(String email);

    /**
     * 인증 코드 삭제
     * @param email 이메일 주소
     */
    void deleteCode(String email);

    /**
     * 인증 완료 상태 저장
     * @param email 이메일 주소
     * @param ttl 유효 시간
     */
    void saveVerified(String email, Duration ttl);

    /**
     * 인증 완료 여부 확인
     * @param email 이메일 주소
     * @return 인증 완료 여부
     */
    boolean isVerified(String email);

    /**
     * 인증 완료 상태 삭제
     * @param email 이메일 주소
     */
    void deleteVerified(String email);
}