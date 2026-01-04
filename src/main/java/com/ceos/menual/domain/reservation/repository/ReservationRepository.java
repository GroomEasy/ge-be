package com.ceos.menual.domain.reservation.repository;

import com.ceos.menual.entity.Reservation;
import com.ceos.menual.entity.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

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
}