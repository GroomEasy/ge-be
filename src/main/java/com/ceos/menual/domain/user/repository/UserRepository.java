package com.ceos.menual.domain.user.repository;

import com.ceos.menual.entity.User;
import com.ceos.menual.entity.enums.AuthProvider;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    /**
     * 회원 탈퇴용 User 조회 (PESSIMISTIC_WRITE 락)
     * 동시성 문제 해결: 탈퇴 처리 중 예약/상담 생성 방지
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

}
