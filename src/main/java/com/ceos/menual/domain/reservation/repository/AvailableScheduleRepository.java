package com.ceos.menual.domain.reservation.repository;

import com.ceos.menual.entity.AvailableSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AvailableScheduleRepository extends JpaRepository<AvailableSchedule, Long> {

    /**
     * 전문가의 특정 날짜 가능 시간 조회
     */
    @Query("SELECT a FROM AvailableSchedule a " +
            "WHERE a.expertProfile.id = :expertProfileId " +
            "AND a.availableDate = :date " +
            "AND a.isActive = true " +
            "ORDER BY a.availableTime")
    List<AvailableSchedule> findActiveSchedulesByExpertProfileIdAndDate(
            @Param("expertProfileId") Long expertProfileId,
            @Param("date") LocalDate date
    );

    /**
     * 전문가의 특정 기간 가능 시간 조회
     */
    @Query("SELECT a FROM AvailableSchedule a " +
            "WHERE a.expertProfile.id = :expertProfileId " +
            "AND a.availableDate BETWEEN :startDate AND :endDate " +
            "AND a.isActive = true " +
            "ORDER BY a.availableDate, a.availableTime")
    List<AvailableSchedule> findActiveSchedulesByExpertProfileIdAndDateRange(
            @Param("expertProfileId") Long expertProfileId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 중복 체크용 (같은 전문가, 같은 날짜, 같은 시간)
     */
    boolean existsByExpertProfileIdAndAvailableDateAndAvailableTimeAndIsActive(
            Long expertProfileId,
            LocalDate availableDate,
            LocalTime availableTime,
            Boolean isActive
    );

    /**
     * 특정 전문가의 특정 날짜에 등록된 모든 시간 조회 (활성/비활성 모두)
     */
    @Query("SELECT a FROM AvailableSchedule a " +
            "WHERE a.expertProfile.id = :expertProfileId " +
            "AND a.availableDate = :date " +
            "ORDER BY a.availableTime")
    List<AvailableSchedule> findAllByExpertProfileIdAndDate(
            @Param("expertProfileId") Long expertProfileId,
            @Param("date") LocalDate date
    );
}