package com.ceos.menual.domain.reservation.repository;

import com.ceos.menual.entity.GeneralProfile;
import com.ceos.menual.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReservationHistoryRepository extends JpaRepository<Reservation, Long> {

    /**
     * 일반 회원의 전체 예약 내역 조회 (최신순)
     */
    @Query("SELECT r FROM Reservation r " +
            "JOIN FETCH r.expertProfile ep " +
            "JOIN FETCH ep.user " +
            "LEFT JOIN FETCH r.consultation " +
            "WHERE r.generalProfile = :generalProfile " +
            "ORDER BY r.createdAt DESC")
    List<Reservation> findByGeneralProfileOrderByCreatedAtDesc(
            @Param("generalProfile") GeneralProfile generalProfile
    );

}
