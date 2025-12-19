package com.ceos.menual.domain.consultation.repository;

import com.ceos.menual.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    // Consultation -> Profile -> User 까지 한 번에 조회 (N+1 방지)
    // expertProfile과 generalProfile을 Fetch Join 하고
    // 그 안의 User(또는 닉네임 정보)까지 Fetch Join 한다고 가정
    @Query("SELECT c FROM Consultation c " +
            "JOIN FETCH c.expertProfile ep " +
            "JOIN FETCH ep.user u1 " +         // ExpertProfile 안의 User
            "JOIN FETCH c.generalProfile gp " +
            "JOIN FETCH gp.user u2 " +         // GeneralProfile 안의 User
            "WHERE c.id = :id")
    Optional<Consultation> findByIdWithProfiles(@Param("id") Long id);

    // Consultation 조회 시 -> ExpertProfile, GeneralProfile, User, Category를 모두 Fetch Join
    @Query("SELECT c FROM Consultation c " +
            "JOIN FETCH c.expertProfile ep " +
            "JOIN FETCH ep.user u1 " +         // 전문가의 User 정보
            "JOIN FETCH ep.category cat " +    // 전문가의 Category 정보
            "JOIN FETCH c.generalProfile gp " +
            "JOIN FETCH gp.user u2 " +         // 의뢰인의 User 정보
            "WHERE c.id = :id")
    Optional<Consultation> findByIdWithAllRelations(@Param("id") Long id);
}