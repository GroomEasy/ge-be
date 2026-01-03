package com.ceos.menual.domain.reservation.service;

import com.ceos.menual.domain.reservation.dto.request.CreateTempReservationRequestDTO;
import com.ceos.menual.domain.reservation.dto.response.TempReservationResponseDTO;
import com.ceos.menual.domain.reservation.exception.ReservationErrorCode;
import com.ceos.menual.domain.reservation.repository.ReservationRepository;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.ExpertProfile;
import com.ceos.menual.entity.GeneralProfile;
import com.ceos.menual.entity.Reservation;
import com.ceos.menual.entity.User;
import com.ceos.menual.entity.enums.ConsultationType;
import com.ceos.menual.entity.enums.ReservationStatus;
import com.ceos.menual.entity.enums.UserType;
import com.ceos.menual.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    private static final int PAYMENT_WAITING_MINUTES = 30;

    @Transactional
    public TempReservationResponseDTO createTempReservation(
            Long userId,
            CreateTempReservationRequestDTO requestDTO
    ) {
        log.info("임시 예약 생성 시작 - userId: {}, expertId: {}",
                userId, requestDTO.getExpertId());

        // 유효성 검증
        validateRequest(requestDTO);

        // 전문가 조회
        User expertUser = userRepository.findById(requestDTO.getExpertId())
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        if (expertUser.getUserType() != UserType.EXPERT || expertUser.getExpertProfile() == null) {
            throw new GlobalException(ReservationErrorCode.EXPERT_PROFILE_NOT_FOUND);
        }

        ExpertProfile expertProfile = expertUser.getExpertProfile();

        // 일반 회원 조회
        User generalUser = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        if (generalUser.getGeneralProfile() == null) {
            throw new GlobalException(ReservationErrorCode.GENERAL_PROFILE_NOT_FOUND);
        }

        GeneralProfile generalProfile = generalUser.getGeneralProfile();

        // 화상 상담인 경우 시간 중복 체크
        if (requestDTO.getConsultationType() == ConsultationType.VIDEO) {
            checkTimeAvailability(expertProfile.getId(), requestDTO.getScheduledDateTime());
        }

        // 임시 예약 생성
        Reservation reservation = Reservation.builder()
                .expertProfile(expertProfile)
                .generalProfile(generalProfile)
                .category(requestDTO.getCategory())
                .consultationType(requestDTO.getConsultationType())
                .scheduledDateTime(requestDTO.getScheduledDateTime())
                .price(requestDTO.getPrice())
                .reservationStatus(ReservationStatus.UNPAID)
                .expiresAt(LocalDateTime.now().plusMinutes(PAYMENT_WAITING_MINUTES))
                .build();

        Reservation saved = reservationRepository.save(reservation);

        log.info("임시 예약 생성 완료 - reservationId: {}, status: {}",
                saved.getId(), saved.getReservationStatus());

        return TempReservationResponseDTO.from(saved);
    }

    private void validateRequest(CreateTempReservationRequestDTO requestDTO) {
        if (requestDTO.getConsultationType() == ConsultationType.VIDEO) {
            if (requestDTO.getScheduledDateTime() == null) {
                throw new GlobalException(ReservationErrorCode.MISSING_SCHEDULED_TIME);
            }
            if (requestDTO.getScheduledDateTime().isBefore(LocalDateTime.now())) {
                throw new GlobalException(ReservationErrorCode.INVALID_RESERVATION_TIME);
            }
        }

        if (requestDTO.getConsultationType() == ConsultationType.MESSAGE
                && requestDTO.getScheduledDateTime() != null) {
            throw new GlobalException(ReservationErrorCode.INVALID_MESSAGE_CONSULTATION);
        }
    }

    private void checkTimeAvailability(Long expertProfileId, LocalDateTime scheduledDateTime) {
        boolean isBooked = reservationRepository.existsByExpertProfileIdAndScheduledDateTime(
                expertProfileId,
                scheduledDateTime
        );

        if (isBooked) {
            log.warn("시간 중복 - expertProfileId: {}, scheduledDateTime: {}",
                    expertProfileId, scheduledDateTime);
            throw new GlobalException(ReservationErrorCode.ALREADY_BOOKED_TIME);
        }
    }
}