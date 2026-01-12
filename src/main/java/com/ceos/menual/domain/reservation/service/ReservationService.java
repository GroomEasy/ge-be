package com.ceos.menual.domain.reservation.service;

import com.ceos.menual.domain.chat.dto.request.ChatroomCreateRequestDTO;
import com.ceos.menual.domain.chat.dto.response.ChatroomResponseDTO;
import com.ceos.menual.domain.chat.service.ChatroomService;
import com.ceos.menual.domain.common.service.S3PresignedUrlService;
import com.ceos.menual.domain.consultation.repository.ConsultationRepository;
import com.ceos.menual.domain.consultation.repository.ConsultationScheduleRepository;
import com.ceos.menual.domain.reservation.dto.ConcernJsonDTO;
import com.ceos.menual.domain.reservation.dto.FashionConcernJsonDTO;
import com.ceos.menual.domain.reservation.dto.FashionImageListDTO;
import com.ceos.menual.domain.reservation.dto.HairConcernJsonDTO;
import com.ceos.menual.domain.reservation.dto.HairImageListDTO;
import com.ceos.menual.domain.reservation.dto.request.CompletePaymentRequestDTO;
import com.ceos.menual.domain.reservation.dto.request.CreateTempReservationRequestDTO;
import com.ceos.menual.domain.reservation.dto.request.UpdateFashionConcernRequestDTO;
import com.ceos.menual.domain.reservation.dto.request.UpdateHairConcernRequestDTO;
import com.ceos.menual.domain.reservation.dto.response.AvailableDatesResponseDTO;
import com.ceos.menual.domain.reservation.dto.response.AvailableTimesResponseDTO;
import com.ceos.menual.domain.reservation.dto.response.CompletePaymentResponseDTO;
import com.ceos.menual.domain.reservation.dto.response.TempReservationResponseDTO;
import com.ceos.menual.domain.reservation.dto.response.UpdateReservationConcernResponseDTO;
import com.ceos.menual.domain.reservation.exception.ReservationErrorCode;
import com.ceos.menual.domain.reservation.repository.AvailableScheduleRepository;
import com.ceos.menual.domain.reservation.repository.ReservationRepository;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.*;
import com.ceos.menual.entity.enums.*;
import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.global.exception.GlobalException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final AvailableScheduleRepository availableScheduleRepository;
    private final ConsultationRepository consultationRepository;
    private final ObjectMapper objectMapper;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final ChatroomService chatroomService;
    private final ConsultationScheduleRepository consultationScheduleRepository;

    private static final List<ReservationStatus> ACTIVE_RESERVATION_STATUSES =
            List.of(ReservationStatus.UNPAID, ReservationStatus.PAID);

    private static final int PAYMENT_WAITING_MINUTES = 60; // 60분 후에 만료

    /**
     * 관리자: 결제 확인 및 Consultation 생성
     * 사용자가 은행 송금으로 입금한 후, 관리자가 확인하면 호출
     * 
     * 동시에 S3의 임시 저장 이미지를 최종 저장 위치로 이동
     * tmp/consultation/user-{userId}/{imageType}/{fileName}
     *     → final/consultation/{consultationId}/{imageType}/{fileName}
     */
    @Transactional
    public CompletePaymentResponseDTO confirmPaymentByAdmin(
            Long reservationId,
            CompletePaymentRequestDTO requestDTO
    ) {
        log.info("관리자 결제 확인 및 상담 생성 시작 - reservationId: {}", reservationId);

        // 예약 조회
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        // 예약 상태 검증 (UNPAID 상태만 결제 가능)
        if (reservation.getReservationStatus() != ReservationStatus.UNPAID) {
            throw new GlobalException(ReservationErrorCode.INVALID_RESERVATION_STATUS);
        }

        // Consultation 생성
        Consultation consultation = Consultation.builder()
                .expertProfile(reservation.getExpertProfile())
                .generalProfile(reservation.getGeneralProfile())
                .type(reservation.getConsultationType())
                .status(ConsultationStatus.READY) // 초기 상태: 준비됨 (결제 확인 후)
                .scheduleTime(reservation.getScheduledDateTime())
                .reviewWritten(false)
                .build();

        Consultation savedConsultation = consultationRepository.save(consultation);

        // 예약 상태를 PAID로 변경하고 consultation과 연결
        reservation.updateStatusToPaid(savedConsultation);

        // S3 임시 이미지를 최종 위치로 이동
        moveImagesToFinalLocation(reservation, savedConsultation);
        
        // 채팅방 생성
        createChatroomsForConsultation(savedConsultation);

        log.info("관리자 결제 확인 및 상담 생성 완료 - reservationId: {}, consultationId: {}",
                reservationId, savedConsultation.getId());

        return CompletePaymentResponseDTO.from(savedConsultation, reservationId);
    }

    /**
     * 예약의 고민지(concernJson) 업데이트
     * 패션 상담: FashionConcernDTO 저장
     * 헤어 상담: HairConcernDTO 저장
     */
    @Transactional
    public UpdateReservationConcernResponseDTO updateFashionConcern(
            Long reservationId,
            Long userId,
            UpdateFashionConcernRequestDTO requestDTO
    ) {
        log.info("패션 상담 고민지 업데이트 시작 - reservationId: {}, userId: {}", 
                reservationId, userId);

        // 예약 조회
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        // 예약 소유자 검증 (예약한 일반 회원과 현재 사용자가 동일한지 확인)
        if (!reservation.getGeneralProfile().getUser().getId().equals(userId)) {
            throw new GlobalException(ReservationErrorCode.UNAUTHORIZED_RESERVATION_ACCESS);
        }

        // 예약의 카테고리가 FASHION인지 확인
        if (!reservation.getCategory().equals(Category.FASHION)) {
            throw new GlobalException(ReservationErrorCode.INVALID_CATEGORY);
        }

        // 패션 상담 고민지 JSON 생성
        if (requestDTO.getFashion() == null) {
            throw new GlobalException(ReservationErrorCode.MISSING_FASHION_CONCERN_DATA);
        }
        
        // 모든 이미지 키를 하나의 리스트로 수집
        List<String> allImageKeys = new ArrayList<>();
        if (requestDTO.getFashion().getImages() != null) {
            if (requestDTO.getFashion().getImages().getFront() != null) {
                allImageKeys.addAll(requestDTO.getFashion().getImages().getFront());
            }
            if (requestDTO.getFashion().getImages().getLeft() != null) {
                allImageKeys.addAll(requestDTO.getFashion().getImages().getLeft());
            }
            if (requestDTO.getFashion().getImages().getRight() != null) {
                allImageKeys.addAll(requestDTO.getFashion().getImages().getRight());
            }
            if (requestDTO.getFashion().getImages().getFavorite() != null) {
                allImageKeys.addAll(requestDTO.getFashion().getImages().getFavorite());
            }
            if (requestDTO.getFashion().getImages().getPurpose() != null) {
                allImageKeys.addAll(requestDTO.getFashion().getImages().getPurpose());
            }
        }
        
        FashionConcernJsonDTO fashionConcern = FashionConcernJsonDTO.builder()
                .type(Category.FASHION.name())
                .fashion(requestDTO.getFashion())
                .build();
        
        log.info("패션 상담 고민지 저장 - reservationId: {}", reservationId);

        // JSON 문자열로 변환
        try {
            String concernJsonString = objectMapper.writeValueAsString(fashionConcern);
            reservation.updateConcerns(concernJsonString);
            log.info("패션 상담 고민지 업데이트 완료 - reservationId: {}", reservationId);
        } catch (Exception e) {
            log.error("고민지 JSON 변환 실패 - reservationId: {}", reservationId, e);
            throw new GlobalException(ReservationErrorCode.CONCERN_JSON_CONVERSION_ERROR);
        }

        return UpdateReservationConcernResponseDTO.from(reservation, fashionConcern);
    }

    @Transactional
    public UpdateReservationConcernResponseDTO updateHairConcern(
            Long reservationId,
            Long userId,
            UpdateHairConcernRequestDTO requestDTO
    ) {
        log.info("헤어 상담 고민지 업데이트 시작 - reservationId: {}, userId: {}", 
                reservationId, userId);

        // 예약 조회
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        // 예약 소유자 검증 (예약한 일반 회원과 현재 사용자가 동일한지 확인)
        if (!reservation.getGeneralProfile().getUser().getId().equals(userId)) {
            throw new GlobalException(ReservationErrorCode.UNAUTHORIZED_RESERVATION_ACCESS);
        }

        // 예약의 카테고리가 HAIR인지 확인
        if (!reservation.getCategory().equals(Category.HAIR)) {
            throw new GlobalException(ReservationErrorCode.INVALID_CATEGORY);
        }

        // 헤어 상담 고민지 JSON 생성
        if (requestDTO.getHair() == null) {
            throw new GlobalException(ReservationErrorCode.MISSING_HAIR_CONCERN_DATA);
        }
        
        // 모든 이미지 키를 하나의 리스트로 수집
        List<String> allImageKeys = new ArrayList<>();
        if (requestDTO.getHair().getImages() != null) {
            if (requestDTO.getHair().getImages().getHairstyle() != null) {
                allImageKeys.addAll(requestDTO.getHair().getImages().getHairstyle());
            }
            if (requestDTO.getHair().getImages().getFront() != null) {
                allImageKeys.addAll(requestDTO.getHair().getImages().getFront());
            }
            if (requestDTO.getHair().getImages().getLeft() != null) {
                allImageKeys.addAll(requestDTO.getHair().getImages().getLeft());
            }
            if (requestDTO.getHair().getImages().getRight() != null) {
                allImageKeys.addAll(requestDTO.getHair().getImages().getRight());
            }
            if (requestDTO.getHair().getImages().getFavorite() != null) {
                allImageKeys.addAll(requestDTO.getHair().getImages().getFavorite());
            }
            if (requestDTO.getHair().getImages().getDifficulty() != null) {
                allImageKeys.addAll(requestDTO.getHair().getImages().getDifficulty());
            }
        }
        
        HairConcernJsonDTO hairConcern = HairConcernJsonDTO.builder()
                .type(Category.HAIR.name())
                .hair(requestDTO.getHair())
                .build();
        
        log.info("헤어 상담 고민지 저장 - reservationId: {}", reservationId);

        // JSON 문자열로 변환
        try {
            String concernJsonString = objectMapper.writeValueAsString(hairConcern);
            reservation.updateConcerns(concernJsonString);
            log.info("헤어 상담 고민지 업데이트 완료 - reservationId: {}", reservationId);
        } catch (Exception e) {
            log.error("고민지 JSON 변환 실패 - reservationId: {}", reservationId, e);
            throw new GlobalException(ReservationErrorCode.CONCERN_JSON_CONVERSION_ERROR);
        }

        return UpdateReservationConcernResponseDTO.from(reservation, hairConcern);
    }

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

        // 해당 타입의 상담 스케줄 찾기 & 가격 결정
        ConsultationSchedule targetSchedule = consultationScheduleRepository
                .findActiveScheduleByExpertProfileIdAndType(
                        expertProfile.getId(),
                        requestDTO.getConsultationType()
                )
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.CONSULTATION_TYPE_NOT_SUPPORTED));

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
                .price(targetSchedule.getPrice())
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

    /**
     * 이미지 키 형식 검증
     * 
     * 기대 형식: tmp/consultation/user-{userId}/{imageType}/{fileName}
     * 예: tmp/consultation/user-123/purpose/1.jpg
     * 
     * @param imageKeys 검증할 이미지 키 목록
     * @return 잘못된 형식의 이미지 키 목록 (비어있으면 모두 유효)
     */
    private List<String> validateImageKeysFormat(List<String> imageKeys) {
        List<String> invalidKeys = new ArrayList<>();

        for (String imageKey : imageKeys) {
            if (!isValidImageKeyFormat(imageKey)) {
                invalidKeys.add(imageKey);
            }
        }

        return invalidKeys;
    }

    /**
     * 단일 이미지 키의 형식이 유효한지 확인
     * 
     * 지원 형식:
     * tmp/consultation/reservation-{resourceId}/{imageType}/{fileName}
     * 
     * @param imageKey 검증할 이미지 키
     * @return 유효하면 true, 아니면 false
     */
    private boolean isValidImageKeyFormat(String imageKey) {
        // null 또는 빈 문자열 체크
        if (imageKey == null || imageKey.trim().isEmpty()) {
            log.warn("빈 이미지 키");
            return false;
        }

        // 경로 분석
        String[] parts = imageKey.split("/");

        // 기대 형식: tmp/consultation/reservation-{resourceId}/{imageType}/{fileName}
        // parts[0] = "tmp"
        // parts[1] = "consultation"
        // parts[2] = "reservation-{resourceId}"
        // parts[3] = "{imageType}"
        // parts[4] = "{fileName}"
        if (parts.length < 5) {
            log.warn("이미지 키 구조 불일치 - imageKey: {}, partCount: {}", imageKey, parts.length);
            return false;
        }

        // 기본 경로 검증
        if (!"tmp".equals(parts[0])) {
            log.warn("첫 번째 경로 컴포넌트가 'tmp'가 아님 - imageKey: {}", imageKey);
            return false;
        }

        if (!"consultation".equals(parts[1])) {
            log.warn("두 번째 경로 컴포넌트가 'consultation'이 아님 - imageKey: {}", imageKey);
            return false;
        }

        // 세 번째 부분: reservation-{resourceId} 형식만 허용
        if (!parts[2].startsWith("reservation-")) {
            log.warn("세 번째 경로 컴포넌트가 'reservation-' 형식이 아님 - imageKey: {}, part: {}", imageKey, parts[2]);
            return false;
        }

        // imageType 검증 (비어있으면 안됨)
        if (parts[3] == null || parts[3].trim().isEmpty()) {
            log.warn("imageType이 비어있음 - imageKey: {}", imageKey);
            return false;
        }

        // fileName 검증 (비어있으면 안됨)
        if (parts[4] == null || parts[4].trim().isEmpty()) {
            log.warn("fileName이 비어있음 - imageKey: {}", imageKey);
            return false;
        }

        return true;
    }

    /**
     * S3 임시 저장된 이미지를 최종 위치로 이동
     * tmp/consultation/user-{userId}/{imageType}/{fileName}
     *     → final/consultation/{consultationId}/{imageType}/{fileName}
     */
    private void moveImagesToFinalLocation(Reservation reservation, Consultation consultation) {
        try {
            // 고민지 JSON 파싱
            String concernsJsonString = reservation.getConcernsJson();
            if (concernsJsonString == null || concernsJsonString.isEmpty()) {
                log.info("이동할 이미지가 없음 - consultationId: {}", consultation.getId());
                return;
            }

            ConcernJsonDTO concernJson = objectMapper.readValue(concernsJsonString, ConcernJsonDTO.class);
            
            // 패션 또는 헤어 상담 고민지에서 이미지 키 추출
            List<String> imageKeys = new ArrayList<>();
            
            if (concernJson.getFashion() != null && concernJson.getFashion().getImages() != null) {
                FashionImageListDTO fashionImages = concernJson.getFashion().getImages();
                if (fashionImages.getFront() != null) imageKeys.addAll(fashionImages.getFront());
                if (fashionImages.getLeft() != null) imageKeys.addAll(fashionImages.getLeft());
                if (fashionImages.getRight() != null) imageKeys.addAll(fashionImages.getRight());
                if (fashionImages.getFavorite() != null) imageKeys.addAll(fashionImages.getFavorite());
                if (fashionImages.getPurpose() != null) imageKeys.addAll(fashionImages.getPurpose());
            } else if (concernJson.getHair() != null && concernJson.getHair().getImages() != null) {
                HairImageListDTO hairImages = concernJson.getHair().getImages();
                if (hairImages.getHairstyle() != null) imageKeys.addAll(hairImages.getHairstyle());
                if (hairImages.getFront() != null) imageKeys.addAll(hairImages.getFront());
                if (hairImages.getLeft() != null) imageKeys.addAll(hairImages.getLeft());
                if (hairImages.getRight() != null) imageKeys.addAll(hairImages.getRight());
                if (hairImages.getFavorite() != null) imageKeys.addAll(hairImages.getFavorite());
                if (hairImages.getDifficulty() != null) imageKeys.addAll(hairImages.getDifficulty());
            }

            if (imageKeys == null || imageKeys.isEmpty()) {
                log.info("이미지 키가 없음 - consultationId: {}", consultation.getId());
                return;
            }

            Long userId = reservation.getGeneralProfile().getUser().getId();
            Long consultationId = consultation.getId();

            // 사전 검증: 모든 이미지 키가 유효한 형식인지 확인 (이동 작업 전)
            List<String> invalidImageKeys = validateImageKeysFormat(imageKeys);
            if (!invalidImageKeys.isEmpty()) {
                log.error("잘못된 이미지 키 형식 발견 - 이동 작업 중단 - consultationId: {}, invalidKeys: {}", 
                    consultationId, invalidImageKeys);
                throw new GlobalException(ReservationErrorCode.INVALID_IMAGE_KEY_FORMAT);
            }

            // 모든 키가 유효한 경우에만 이미지 이동 시작
            for (String imageKey : imageKeys) {
                String[] parts = imageKey.split("/");
                
                if (parts.length < 4) {
                    log.error("예상치 못한 이미지 키 형식 - consultationId: {}, imageKey: {}", 
                        consultationId, imageKey);
                    throw new GlobalException(ReservationErrorCode.INVALID_IMAGE_KEY_FORMAT);
                }

                // reservation ID 검증
                String reservationIdPart = parts[2];
                if (!reservationIdPart.startsWith("reservation-")) {
                    log.error("잘못된 예약 ID 형식 - consultationId: {}, imageKey: {}", 
                        consultationId, imageKey);
                    throw new GlobalException(ReservationErrorCode.INVALID_IMAGE_KEY_FORMAT);
                }

                String imageType = parts[3];
                String fileName = parts[4];

                String finalS3Key = String.format("final/consultation/%d/%s/%s", 
                    consultationId, imageType, fileName);

                s3PresignedUrlService.moveImageFromTempToFinal(imageKey, finalS3Key);

                log.info("이미지 이동 완료 - from: {}, to: {}", imageKey, finalS3Key);
            }

            // ConcernJsonDTO의 imageKeys를 최종 경로로 업데이트
            // 모든 이미지 키는 이미 검증되었으므로 안전하게 변환
            List<String> finalImageKeys = imageKeys.stream()
                    .map(imageKey -> {
                        String[] parts = imageKey.split("/");
                        // 사전 검증에서 이미 확인했으므로 parts.length >= 4 보장
                        String imageType = parts[3];
                        String fileName = parts[4];
                        return String.format("final/consultation/%d/%s/%s", 
                            consultationId, imageType, fileName);
                    })
                    .collect(Collectors.toList());

            // 기존 ConcernJsonDTO 데이터를 유지하고 이미지 경로만 업데이트
            Map<String, String> imagePathMapping = new HashMap<>();
            for (int i = 0; i < imageKeys.size(); i++) {
                imagePathMapping.put(imageKeys.get(i), finalImageKeys.get(i));
            }

            if (concernJson.getFashion() != null && concernJson.getFashion().getImages() != null) {
                FashionImageListDTO fashionImages = concernJson.getFashion().getImages();
                FashionImageListDTO updatedFashionImages = FashionImageListDTO.builder()
                        .front(updateImagePaths(fashionImages.getFront(), imagePathMapping))
                        .left(updateImagePaths(fashionImages.getLeft(), imagePathMapping))
                        .right(updateImagePaths(fashionImages.getRight(), imagePathMapping))
                        .favorite(updateImagePaths(fashionImages.getFavorite(), imagePathMapping))
                        .purpose(updateImagePaths(fashionImages.getPurpose(), imagePathMapping))
                        .build();
                
                // FashionConcernDTO 재구성
                com.ceos.menual.domain.reservation.dto.FashionConcernDTO updatedFashion = 
                    com.ceos.menual.domain.reservation.dto.FashionConcernDTO.builder()
                        .height(concernJson.getFashion().getHeight())
                        .weight(concernJson.getFashion().getWeight())
                        .topSize(concernJson.getFashion().getTopSize())
                        .bottomSize(concernJson.getFashion().getBottomSize())
                        .bodyTypeDisadvantages(concernJson.getFashion().getBodyTypeDisadvantages())
                        .bodyTypeEtcText(concernJson.getFashion().getBodyTypeEtcText())
                        .styleColors(concernJson.getFashion().getStyleColors())
                        .styleFits(concernJson.getFashion().getStyleFits())
                        .styleImages(concernJson.getFashion().getStyleImages())
                        .styleEtcText(concernJson.getFashion().getStyleEtcText())
                        .outfitItems(concernJson.getFashion().getOutfitItems())
                        .outfitPriceRange(concernJson.getFashion().getOutfitPriceRange())
                        .outfitEtcText(concernJson.getFashion().getOutfitEtcText())
                        .images(updatedFashionImages)
                        .build();
                
                concernJson = ConcernJsonDTO.builder()
                        .type(concernJson.getType())
                        .fashion(updatedFashion)
                        .hair(concernJson.getHair())
                        .desiredStyle(concernJson.getDesiredStyle())
                        .consultationPurpose(concernJson.getConsultationPurpose())
                        .build();
            } else if (concernJson.getHair() != null && concernJson.getHair().getImages() != null) {
                HairImageListDTO hairImages = concernJson.getHair().getImages();
                HairImageListDTO updatedHairImages = HairImageListDTO.builder()
                        .hairstyle(updateImagePaths(hairImages.getHairstyle(), imagePathMapping))
                        .front(updateImagePaths(hairImages.getFront(), imagePathMapping))
                        .left(updateImagePaths(hairImages.getLeft(), imagePathMapping))
                        .right(updateImagePaths(hairImages.getRight(), imagePathMapping))
                        .favorite(updateImagePaths(hairImages.getFavorite(), imagePathMapping))
                        .difficulty(updateImagePaths(hairImages.getDifficulty(), imagePathMapping))
                        .build();
                
                // HairConcernDTO 재구성
                com.ceos.menual.domain.reservation.dto.HairConcernDTO updatedHair = 
                    com.ceos.menual.domain.reservation.dto.HairConcernDTO.builder()
                        .faceAdvantages(concernJson.getHair().getFaceAdvantages())
                        .faceAdvantagesEtcText(concernJson.getHair().getFaceAdvantagesEtcText())
                        .coveringParts(concernJson.getHair().getCoveringParts())
                        .coveringPartsEtcText(concernJson.getHair().getCoveringPartsEtcText())
                        .pursuedImages(concernJson.getHair().getPursuedImages())
                        .stylingDifficulty(concernJson.getHair().getStylingDifficulty())
                        .images(updatedHairImages)
                        .build();
                
                concernJson = ConcernJsonDTO.builder()
                        .type(concernJson.getType())
                        .fashion(concernJson.getFashion())
                        .hair(updatedHair)
                        .desiredStyle(concernJson.getDesiredStyle())
                        .consultationPurpose(concernJson.getConsultationPurpose())
                        .build();
            }

            // 업데이트된 JSON으로 저장
            String updatedConcernsJsonString = objectMapper.writeValueAsString(concernJson);
            reservation.updateConcerns(updatedConcernsJsonString);

            log.info("고민지 이미지 경로 업데이트 완료 - consultationId: {}", consultationId);

        } catch (Exception e) {
            log.error("S3 이미지 이동 중 오류 발생 - consultationId: {}", consultation.getId(), e);
            throw new GlobalException(ReservationErrorCode.CONCERN_JSON_CONVERSION_ERROR);
        }
    }

    /**
     * 상담 확정 시 채팅방 자동 생성
     * - 상담 타입에 따라 MESSAGE 또는 VIDEO 채팅방 생성
     */
    @Transactional
    public void createChatroomsForConsultation(Consultation consultation) {
        log.info("채팅방 자동 생성 시작 - consultationId: {}, type: {}",
                consultation.getId(), consultation.getType());

        // 전문가 ID 추출 (채팅방 생성 주체)
        Long expertId = consultation.getExpertProfile().getUser().getId();
        Long consultationId = consultation.getId();

        // 상담 타입에 따라 채팅방 생성
        if (consultation.getType() == ConsultationType.MESSAGE) {
            // 메시지 상담 MESSAGE 채팅방 생성
            createChatroom(expertId, consultationId, ChatroomType.MESSAGE);

        } else if (consultation.getType() == ConsultationType.VIDEO) {
            // 화상 상담 VIDEO 채팅방 생성
            createChatroom(expertId, consultationId, ChatroomType.VIDEO);
        }

        log.info("채팅방 자동 생성 완료 - consultationId: {}", consultationId);
    }

    /**
     * 특정 타입의 채팅방 생성
     */
    private void createChatroom(Long expertId, Long consultationId, ChatroomType chatroomType) {
        ChatroomCreateRequestDTO request = ChatroomCreateRequestDTO.builder()
                .consultationId(consultationId)
                .chatroomType(chatroomType)
                .build();

            ChatroomResponseDTO chatroom = chatroomService.createChatroom(expertId, consultationId, request);
            log.info("채팅방 생성 성공 - chatroomId: {}, type: {}", chatroom.getChatroomId(), chatroomType);
    }

    /**
     * 이미지 경로 목록을 업데이트하는 헬퍼 메서드
     */
    private List<String> updateImagePaths(List<String> imagePaths, Map<String, String> imagePathMapping) {
        if (imagePaths == null) {
            return null;
        }
        return imagePaths.stream()
                .map(oldPath -> imagePathMapping.getOrDefault(oldPath, oldPath))
                .collect(Collectors.toList());
    }
}