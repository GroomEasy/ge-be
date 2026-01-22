package com.ceos.menual.domain.consultation.repository;

import com.ceos.menual.entity.Consultation;
import com.ceos.menual.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
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

    /**
     * 상담 시간 10분 전에 Zoom 링크 메시지를 전송해야 하는 화상 상담 조회
     * 조건: VIDEO 타입, READY 상태, Zoom 미팅 생성됨, 링크 미전송, 상담시간이 범위 내
     */
    @Query("SELECT c FROM Consultation c " +
            "LEFT JOIN FETCH c.expertProfile ep " +
            "LEFT JOIN FETCH ep.user " +
            "LEFT JOIN FETCH c.generalProfile gp " +
            "LEFT JOIN FETCH gp.user " +
            "WHERE c.type = 'VIDEO' " +
            "AND c.status = 'READY' " +
            "AND c.isZoomMeetingCreated = true " +
            "AND (c.zoomLinkSent = false OR c.zoomLinkSent IS NULL) " +
            "AND c.scheduleTime BETWEEN :startTime AND :endTime")
    List<Consultation> findVideoConsultationsForZoomLinkNotification(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}