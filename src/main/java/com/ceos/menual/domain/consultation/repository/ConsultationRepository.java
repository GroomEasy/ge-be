package com.ceos.menual.domain.consultation.repository;

import com.ceos.menual.entity.Consultation;
import com.ceos.menual.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<Consultation, Long>, ConsultationRepositoryCustom {


    @Query("SELECT c FROM Consultation c " +
            "LEFT JOIN FETCH c.expertProfile ep " +
            "LEFT JOIN FETCH ep.user u1 " +         // ExpertProfile 안의 User
            "LEFT JOIN FETCH c.generalProfile gp " +
            "LEFT JOIN FETCH gp.user u2 " +         // GeneralProfile 안의 User
            "WHERE c.id = :id")
    Optional<Consultation> findByIdWithProfiles(@Param("id") Long id);

    @Query("SELECT c FROM Consultation c " +
            "LEFT JOIN FETCH c.expertProfile ep " +
            "LEFT JOIN FETCH ep.user u1 " +         // 전문가의 User 정보
            "LEFT JOIN FETCH c.generalProfile gp " +
            "LEFT JOIN FETCH gp.user u2 " +         // 의뢰인의 User 정보
            "WHERE c.id = :id")
    Optional<Consultation> findByIdWithAllRelations(@Param("id") Long id);


    /**
     * consultationId로 Reservation 조회
     */
    @Query("SELECT r FROM Reservation r WHERE r.consultation.id = :consultationId")
    Optional<Reservation> findByConsultationId(@Param("consultationId") Long consultationId);

    /**
     * 일반 회원의 진행 중인 상담 존재 여부 확인
     * (IN_PROGRESS, READY 상태 = 아직 완료되지 않은 상담)
     */
    @Query("SELECT COUNT(c) > 0 FROM Consultation c " +
            "WHERE c.generalProfile.id = :generalProfileId " +
            "AND c.status IN ('IN_PROGRESS', 'READY')")
    boolean existsActiveConsultationByGeneralProfileId(
            @Param("generalProfileId") Long generalProfileId
    );

    /**
     * 전문가의 진행 중인 상담 존재 여부 확인
     */
    @Query("SELECT COUNT(c) > 0 FROM Consultation c " +
            "WHERE c.expertProfile.id = :expertProfileId " +
            "AND c.status IN ('IN_PROGRESS', 'READY')")
    boolean existsActiveConsultationByExpertProfileId(
            @Param("expertProfileId") Long expertProfileId
    );
}