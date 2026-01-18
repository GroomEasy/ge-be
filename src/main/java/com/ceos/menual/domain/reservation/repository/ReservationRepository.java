package com.ceos.menual.domain.reservation.repository;

import com.ceos.menual.entity.Reservation;
import com.ceos.menual.entity.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * 특정 전문가의 특정 시간에 예약 중복 체크
     * UNPAID, PAID 상태만 체크 -> 다른 상태들은 예약이 아님 (REFUND_REQUESTED, REFUNDED)
     */
    @Query("SELECT COUNT(r) > 0 FROM Reservation r " +
            "WHERE r.expertProfile.id = :expertProfileId " +
            "AND r.scheduledDateTime = :scheduledDateTime " +
            "AND r.reservationStatus IN (:unpaid, :paid)") // UNPAID, PAID 만 예약 완료 상태임
    boolean existsByExpertProfileIdAndScheduledDateTime(
            @Param("expertProfileId") Long expertProfileId,
            @Param("scheduledDateTime") LocalDateTime scheduledDateTime,
            @Param("unpaid") ReservationStatus unpaid,
            @Param("paid") ReservationStatus paid
    );

    /**
     * 특정 전문가의 특정 기간 내 예약된 시간 조회
     */
    @Query("SELECT r.scheduledDateTime FROM Reservation r " +
            "WHERE r.expertProfile.id = :expertProfileId " +
            "AND r.scheduledDateTime BETWEEN :startDateTime AND :endDateTime " +
            "AND r.reservationStatus IN :statuses " +
            "ORDER BY r.scheduledDateTime")
    List<LocalDateTime> findBookedTimesByExpertProfileIdAndDateRange(
            @Param("expertProfileId") Long expertProfileId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("statuses") List<ReservationStatus> statuses
    );

    /**
     * 만료된 미입금 예약 조회 (스케줄러용)
     */
    List<Reservation> findByReservationStatusAndExpiresAtBefore(
            ReservationStatus status,
            LocalDateTime expiresAt
    );

    /**
     * 결제 확인용 Reservation 조회 (PESSIMISTIC_WRITE 락)
     * 
     * 동시성 문제 해결:
     * - 여러 관리자가 동시에 confirmPaymentByAdmin()을 호출해도
     * - 첫 번째 트랜잭션만 성공, 나머지는 대기 후 INVALID_RESERVATION_STATUS 예외 발생
     * - Consultation 중복 생성 방지
     * 
     * @param reservationId 예약 ID
     * @return 배타적 락이 획득된 Reservation
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") Long reservationId);

    /**
     * 원자적 상태 업데이트: UNPAID → PAID
     * 
     * SQL 레벨에서 원자성 보장:
     * - WHERE 조건에서 UNPAID 상태만 대상
     * - 업데이트되는 행이 정확히 1개만 보장
     * - 레이스 컨디션 방지
     * 
     * @param reservationId 예약 ID
     * @param consultationId 생성된 상담 ID
     * @return 업데이트된 행 수 (1 = 성공, 0 = 다른 상태 또는 이미 업데이트됨)
     */
    @Modifying
    @Query("UPDATE Reservation r SET r.reservationStatus = 'PAID', " +
            "r.consultation.id = :consultationId, " +
            "r.expiresAt = null, " +
            "r.paidAt = CURRENT_TIMESTAMP " +
            "WHERE r.id = :reservationId AND r.reservationStatus = 'UNPAID'")
    int updateStatusToPaidIfUnpaid(
            @Param("reservationId") Long reservationId,
            @Param("consultationId") Long consultationId
    );

    /**
     * consultationId로 Reservation 조회
     */
    Optional<Reservation> findByConsultationId(Long consultationId);

    /**
     * 사용자의 결제 완료된 예약 내역 조회 (전체)
     * PAID 상태인 예약만 조회, 최신순 정렬
     */
    @Query("SELECT r FROM Reservation r " +
            "JOIN FETCH r.expertProfile ep " +
            "JOIN FETCH ep.user " +
            "WHERE r.generalProfile.user.id = :userId " +
            "AND r.reservationStatus IN ('PAID', 'REFUNDED') " +
            "ORDER BY r.paidAt DESC, r.updatedAt DESC")
    List<Reservation> findPaymentHistoryByUserId(
            @Param("userId") Long userId
    );

    /**
     * 사용자의 결제 완료된 예약 내역 조회 (카테고리별)
     * PAID 상태이고 특정 카테고리인 예약만 조회, 최신순 정렬
     */
    @Query("SELECT r FROM Reservation r " +
            "JOIN FETCH r.expertProfile ep " +
            "JOIN FETCH ep.user " +
            "WHERE r.generalProfile.user.id = :userId " +
            "AND r.reservationStatus IN ('PAID', 'REFUNDED') " +
            "AND r.category = :category " +
            "ORDER BY r.paidAt DESC, r.updatedAt DESC")
    List<Reservation> findPaymentHistoryByUserIdAndCategory(
            @Param("userId") Long userId,
            @Param("category") com.ceos.menual.entity.enums.Category category
    );
}