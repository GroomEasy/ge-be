package com.ceos.menual.domain.consultation.repository;

import com.ceos.menual.entity.Consultation;
import com.ceos.menual.entity.ConsultationSchedule;
import com.ceos.menual.entity.enums.ConsultationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConsultationScheduleRepository extends JpaRepository<Consultation, Long> {

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
     * 전문가가 제공하는 활성화된 상담 스케줄 목록 조회
     */
    @Query("SELECT cs FROM ConsultationSchedule cs " +
            "WHERE cs.expertProfile.id = :expertProfileId " +
            "AND cs.isActive = true")
    List<ConsultationSchedule> findActiveSchedulesByExpertProfileId(
            @Param("expertProfileId") Long expertProfileId
    );

    /**
     * 전문가의 특정 상담 유형 스케줄 조회 (활성화된 것만)
     */
    @Query("SELECT cs FROM ConsultationSchedule cs " +
            "WHERE cs.expertProfile.id = :expertProfileId " +
            "AND cs.consultationType = :consultationType " +
            "AND cs.isActive = true")
    Optional<ConsultationSchedule> findActiveScheduleByExpertProfileIdAndType(
            @Param("expertProfileId") Long expertProfileId,
            @Param("consultationType") ConsultationType consultationType
    );

}