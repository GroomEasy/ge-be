package com.ceos.menual.domain.reservation.service;

import com.ceos.menual.domain.reservation.dto.ReservationHistoryDTO;
import com.ceos.menual.domain.reservation.dto.response.ReservationHistoryListResponseDTO;
import com.ceos.menual.domain.reservation.exception.ReservationErrorCode;
import com.ceos.menual.domain.reservation.repository.ReservationHistoryRepository;
import com.ceos.menual.domain.reservation.repository.ReservationRepository;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.GeneralProfile;
import com.ceos.menual.entity.Reservation;
import com.ceos.menual.entity.User;
import com.ceos.menual.entity.enums.ReservationStatus;
import com.ceos.menual.global.exception.GlobalException;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationHistoryService {

    private final UserRepository userRepository;
    private final ReservationHistoryRepository reservationHistoryRepository;

    public ReservationHistoryListResponseDTO getReservationHistory(Long userId) {
        log.info("예약 내역 조회 시작 - userId: {}", userId);

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        // GeneralProfile 조회
        if (user.getGeneralProfile() == null) {
            throw new GlobalException(ReservationErrorCode.GENERAL_PROFILE_NOT_FOUND);
        }

        GeneralProfile generalProfile = user.getGeneralProfile();

        // 전체 예약 내역 조회
        List<Reservation> reservations = reservationHistoryRepository
                .findByGeneralProfileOrderByCreatedAtDesc(generalProfile);

        // 상태별로 분류
        LocalDateTime now = LocalDateTime.now();

        List<ReservationHistoryDTO> unpaidReservations = new ArrayList<>();
        List<ReservationHistoryDTO> upcomingReservations = new ArrayList<>();

        for (Reservation reservation : reservations) {
            ReservationStatus status = reservation.getReservationStatus();
            LocalDateTime scheduledDateTime = reservation.getScheduledDateTime();

            if (status == ReservationStatus.UNPAID) {
                unpaidReservations.add(ReservationHistoryDTO.from(reservation));
            }
            else if (status == ReservationStatus.PAID && scheduledDateTime != null && scheduledDateTime.isAfter(now)) {
                upcomingReservations.add(ReservationHistoryDTO.from(reservation));
            }
        }

        log.info("예약 내역 조회 완료 - 확정대기: {}, 다가오는예약: {}",
                unpaidReservations.size(), upcomingReservations.size());

        return ReservationHistoryListResponseDTO.of(
                unpaidReservations,
                upcomingReservations
        );
    }
}
