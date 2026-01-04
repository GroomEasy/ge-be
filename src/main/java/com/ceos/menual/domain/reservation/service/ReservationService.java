package com.ceos.menual.domain.reservation.service;

import com.ceos.menual.domain.reservation.dto.request.CreateTempReservationRequestDTO;
import com.ceos.menual.domain.reservation.dto.response.AvailableDatesResponseDTO;
import com.ceos.menual.domain.reservation.dto.response.AvailableTimesResponseDTO;
import com.ceos.menual.domain.reservation.dto.response.TempReservationResponseDTO;
import com.ceos.menual.domain.reservation.exception.ReservationErrorCode;
import com.ceos.menual.domain.reservation.repository.AvailableScheduleRepository;
import com.ceos.menual.domain.reservation.repository.ReservationRepository;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.*;
import com.ceos.menual.entity.enums.ConsultationType;
import com.ceos.menual.entity.enums.ReservationStatus;
import com.ceos.menual.entity.enums.UserType;
import com.ceos.menual.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final AvailableScheduleRepository availableScheduleRepository;

    private static final List<ReservationStatus> ACTIVE_RESERVATION_STATUSES =
            List.of(ReservationStatus.UNPAID, ReservationStatus.PAID);

    private static final int PAYMENT_WAITING_MINUTES = 60; // 60분 후에 만료

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
                scheduledDateTime,
                ReservationStatus.UNPAID,
                ReservationStatus.PAID
        );

        if (isBooked) {
            log.warn("시간 중복 - expertProfileId: {}, scheduledDateTime: {}",
                    expertProfileId, scheduledDateTime);
            throw new GlobalException(ReservationErrorCode.ALREADY_BOOKED_TIME);
        }
    }


    /**
     * 월별 예약 가능 날짜 조회
     */
    public AvailableDatesResponseDTO getAvailableDates(Long expertId, int year, int month) {
        log.info("월별 예약 가능 날짜 조회 - expertId: {}, year: {}, month: {}", expertId, year, month);

        // 전문가 검증
        User expertUser = validateExpert(expertId);
        Long expertProfileId = expertUser.getExpertProfile().getId();

        // 해당 월의 시작일과 종료일
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        // 전문가가 등록한 가능 스케줄 조회
        List<AvailableSchedule> schedules = availableScheduleRepository
                .findActiveSchedulesByExpertProfileIdAndDateRange(
                        expertProfileId,
                        startDate,
                        endDate
                );

        // 날짜별로 그룹핑하여 예약 가능한 날짜만 추출
        List<LocalDate> availableDates = schedules.stream()
                .map(AvailableSchedule::getAvailableDate)
                .distinct()
                .filter(date -> !date.isBefore(LocalDate.now())) // 과거 날짜 제외
                .filter(date -> hasAvailableTimeSlots(expertProfileId, date)) // 예약 가능 시간이 있는지 확인
                .sorted()
                .collect(Collectors.toList());

        log.info("예약 가능 날짜 조회 완료 - 총 {}일", availableDates.size());

        return AvailableDatesResponseDTO.builder()
                .year(year)
                .month(month)
                .availableDates(availableDates)
                .build();
    }

    /**
     * 일별 예약 가능 시간 조회
     */
    public AvailableTimesResponseDTO getAvailableTimes(Long expertId, LocalDate date) {
        log.info("일별 예약 가능 시간 조회 - expertId: {}, date: {}", expertId, date);

        // 전문가 검증
        User expertUser = validateExpert(expertId);
        Long expertProfileId = expertUser.getExpertProfile().getId();

        // 과거 날짜 체크
        if (date.isBefore(LocalDate.now())) {
            throw new GlobalException(ReservationErrorCode.INVALID_RESERVATION_TIME);
        }

        // 전문가가 등록한 해당 날짜의 가능 시간 조회
        List<AvailableSchedule> schedules = availableScheduleRepository
                .findActiveSchedulesByExpertProfileIdAndDate(expertProfileId, date);

        if (schedules.isEmpty()) {
            // 전문가가 해당 날짜에 일하지 않음
            return AvailableTimesResponseDTO.builder()
                    .date(date)
                    .availableTimes(List.of())
                    .build();
        }

        // 전문가가 등록한 시간들
        List<LocalTime> registeredTimes = schedules.stream()
                .map(AvailableSchedule::getAvailableTime)
                .sorted()
                .collect(Collectors.toList());

        // 이미 예약된 시간 조회
        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.atTime(LocalTime.MAX);

        List<LocalDateTime> bookedTimes = reservationRepository
                .findBookedTimesByExpertProfileIdAndDateRange(
                        expertProfileId,
                        startDateTime,
                        endDateTime,
                        ACTIVE_RESERVATION_STATUSES
                );

        // 예약 가능 시간 = 등록된 시간 - 예약된 시간
        List<LocalTime> availableTimes = registeredTimes.stream()
                .filter(time -> {
                    LocalDateTime dateTime = date.atTime(time);

                    // 과거 시간 제외
                    if (dateTime.isBefore(LocalDateTime.now())) {
                        return false;
                    }

                    // 이미 예약된 시간 제외
                    return !bookedTimes.contains(dateTime);
                })
                .collect(Collectors.toList());

        log.info("예약 가능 시간 조회 완료 - 총 {}개 시간대", availableTimes.size());

        return AvailableTimesResponseDTO.builder()
                .date(date)
                .availableTimes(availableTimes)
                .build();
    }

    // ========== 헬퍼 메서드 ========== //

    /**
     * 전문가 검증
     */
    private User validateExpert(Long expertId) {
        User expertUser = userRepository.findById(expertId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        if (expertUser.getUserType() != UserType.EXPERT || expertUser.getExpertProfile() == null) {
            throw new GlobalException(ReservationErrorCode.EXPERT_PROFILE_NOT_FOUND);
        }

        return expertUser;
    }

    /**
     * 특정 날짜에 예약 가능한 시간이 있는지 확인
     */
    private boolean hasAvailableTimeSlots(Long expertProfileId, LocalDate date) {
        // 전문가가 등록한 시간 조회
        List<AvailableSchedule> schedules = availableScheduleRepository
                .findActiveSchedulesByExpertProfileIdAndDate(expertProfileId, date);

        if (schedules.isEmpty()) {
            return false;
        }

        // 이미 예약된 시간 조회
        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.atTime(LocalTime.MAX);

        List<LocalDateTime> bookedTimes = reservationRepository
                .findBookedTimesByExpertProfileIdAndDateRange(
                        expertProfileId,
                        startDateTime,
                        endDateTime,
                        ACTIVE_RESERVATION_STATUSES
                );

        // 예약 가능한 시간이 하나라도 있는지 확인
        LocalDateTime now = LocalDateTime.now();

        return schedules.stream()
                .anyMatch(schedule -> {
                    LocalDateTime dateTime = date.atTime(schedule.getAvailableTime());
                    return dateTime.isAfter(now) && !bookedTimes.contains(dateTime);
                });
    }
}