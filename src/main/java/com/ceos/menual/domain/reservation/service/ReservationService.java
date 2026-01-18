package com.ceos.menual.domain.reservation.service;

import com.ceos.menual.domain.chat.dto.request.ChatroomCreateRequestDTO;
import com.ceos.menual.domain.chat.dto.response.ChatroomResponseDTO;
import com.ceos.menual.domain.chat.repository.ChatroomRepository;
import com.ceos.menual.domain.chat.service.ChatMessageService;
import com.ceos.menual.domain.chat.service.ChatroomService;
import com.ceos.menual.domain.common.service.S3PresignedUrlService;
import com.ceos.menual.domain.consultation.exception.ConsultationErrorCode;
import com.ceos.menual.domain.consultation.repository.ConsultationRepository;
import com.ceos.menual.domain.consultation.repository.ConsultationScheduleRepository;
import com.ceos.menual.domain.expert.exception.ExpertErrorCode;
import com.ceos.menual.domain.expert.repository.ExpertRepository;
import com.ceos.menual.domain.expert.service.zoom.ZoomMeetingService;
import com.ceos.menual.domain.reservation.dto.ConcernJsonDTO;
import com.ceos.menual.domain.reservation.dto.FashionConcernJsonDTO;
import com.ceos.menual.domain.reservation.dto.FashionImageListDTO;
import com.ceos.menual.domain.reservation.dto.HairConcernJsonDTO;
import com.ceos.menual.domain.reservation.dto.HairImageListDTO;
import com.ceos.menual.domain.reservation.dto.request.CompletePaymentRequestDTO;
import com.ceos.menual.domain.reservation.dto.request.CreateTempReservationRequestDTO;
import com.ceos.menual.domain.reservation.dto.request.UpdateFashionConcernRequestDTO;
import com.ceos.menual.domain.reservation.dto.request.UpdateHairConcernRequestDTO;
import com.ceos.menual.domain.reservation.dto.response.*;
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
import java.util.*;
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
    private final ChatMessageService chatMessageService;
    private final ChatroomRepository chatroomRepository;
    private final ZoomMeetingService zoomMeetingService;
    private final com.ceos.menual.global.config.slack.SlackNotificationService slackNotificationService;
    private final ExpertRepository expertRepository;
    private final com.ceos.menual.domain.user.repository.PointHistoryRepository pointHistoryRepository;

    private static final List<ReservationStatus> ACTIVE_RESERVATION_STATUSES =
            List.of(ReservationStatus.UNPAID, ReservationStatus.PAID);

    private static final int PAYMENT_WAITING_MINUTES = 60; // 60분 후에 만료

    /**
     * 관리자: 결제 확정 처리(무통장 입금 확인)
     *
     * 흐름:
     * - 사용자가 입금(은행 송금) → 관리자가 결제 확정 버튼 클릭
     * - 아래 작업들을 하나의 트랜잭션(@Transactional) 안에서 순서대로 수행
     *
     * 수행 작업:
     * - 예약 상태 변경: UNPAID → PAID, 예약에 Consultation 연결
     * - 상담(Consultation) 생성: 타입에 따라 초기 상태 설정(메시지=IN_PROGRESS, 화상=READY)
     * - S3 이미지 이동: 임시(tmp) → 최종(final) 경로로 이동하고 고민지 JSON의 이미지 경로 갱신
     * - 채팅 후처리: 상담 채팅방 생성, 고민지 메시지 전송, 관리자→전문가 알림 메시지 전송
     * - (화상 상담일 때만) Zoom 미팅 생성 및 링크 저장
     *
     * 동시성 안전성:
     * - PESSIMISTIC_WRITE 락으로 동일 reservationId에 대한 동시 결제 확정을 직렬화
     * - 첫 번째 요청만 성공, 이후 요청은 상태 재검증 후 INVALID_RESERVATION_STATUS로 실패
     * - 결과적으로 Consultation 중복 생성/중복 결제 확정을 방지
     */
    @Transactional
    public CompletePaymentResponseDTO confirmPaymentByAdmin(
            Long reservationId,
            CompletePaymentRequestDTO requestDTO,
            Long adminUserId
    ) {
        log.info("관리자 결제 확인 및 상담 생성 시작 - reservationId: {}", reservationId);

        // PESSIMISTIC_WRITE 락을 사용한 예약 조회
        // - 동시 요청 중 첫 번째만 성공
        // - 나머지는 대기 후 상태 재확인
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        // 예약 상태 검증 (UNPAID 또는 SUBMITTED 상태만 결제 가능)
        // 락 획득 후 상태 재확인 (락 대기 중 다른 트랜잭션이 상태를 변경했을 수 있음)
        if (reservation.getReservationStatus() != ReservationStatus.UNPAID
                && reservation.getReservationStatus() != ReservationStatus.SUBMITTED) {
            log.warn("예약 상태 불일치 (동시성 처리됨) - reservationId: {}, status: {}",
                reservationId, reservation.getReservationStatus());
            throw new GlobalException(ReservationErrorCode.INVALID_RESERVATION_STATUS);
        }

        // 상담 타입에 따른 초기 상태 결정
        ConsultationStatus initialStatus;
        if (reservation.getConsultationType() == ConsultationType.MESSAGE) {
            // 메시지 상담: 즉시 진행 중
            initialStatus = ConsultationStatus.IN_PROGRESS;
            log.info("메시지 상담 - 초기 상태: IN_PROGRESS - reservationId: {}", reservationId);
        } else if (reservation.getConsultationType() == ConsultationType.VIDEO) {
            // 화상 상담: 예약된 시간까지 대기
            initialStatus = ConsultationStatus.READY;
            log.info("화상 상담 - 초기 상태: READY - reservationId: {}", reservationId);
        } else {
            log.error("알 수 없는 상담 타입 - reservationId: {}, type: {}",
                    reservationId, reservation.getConsultationType());
            throw new GlobalException(ReservationErrorCode.INVALID_CONSULTATION_TYPE);
        }

        // Consultation 생성
        Consultation consultation = Consultation.builder()
                .expertProfile(reservation.getExpertProfile())
                .generalProfile(reservation.getGeneralProfile())
                .type(reservation.getConsultationType())
                .status(initialStatus)  // 상담 타입에 따른 초기 상태
                .scheduleTime(reservation.getScheduledDateTime())
                .reviewWritten(false)
                .build();

        Consultation savedConsultation = consultationRepository.save(consultation);
        log.info("Consultation 생성 완료 - consultationId: {}, reservationId: {}",
            savedConsultation.getId(), reservationId);

        // 예약 상태를 PAID로 변경하고 consultation과 연결
        reservation.updateStatusToPaid(savedConsultation);
        reservationRepository.save(reservation);
        log.info("예약 상태 업데이트 완료 - UNPAID → PAID, reservationId: {}", reservationId);

        // 포인트 차감 처리
        Integer pointsToDeduct = reservation.getPointsToUse();
        if (pointsToDeduct != null && pointsToDeduct > 0) {
            GeneralProfile generalProfile = reservation.getGeneralProfile();
            User user = generalProfile.getUser();

            // 포인트 차감
            generalProfile.deductPoints(pointsToDeduct);

            // 포인트 히스토리 기록 (음수로 저장)
            PointHistory pointHistory = PointHistory.builder()
                    .user(user)
                    .point(-pointsToDeduct)  // 차감은 음수로 기록
                    .description("포인트 사용 (예약 ID: " + reservationId + ")")
                    .build();
            pointHistoryRepository.save(pointHistory);

            log.info("포인트 차감 완료 - reservationId: {}, deductedPoints: {}, remainingPoints: {}",
                    reservationId, pointsToDeduct, generalProfile.getTotalPoints());
        } else {
            log.info("차감할 포인트 없음 - reservationId: {}, pointsToUse: {}", reservationId, pointsToDeduct);
        }

        // S3 임시 이미지를 최종 위치로 이동
        moveImagesToFinalLocation(reservation, savedConsultation);

        // 채팅방 자동 생성 및 고민지 전송
        createChatroomsAndSendConcern(savedConsultation, reservation, adminUserId);

        // 화상 상담의 경우 Zoom 미팅 생성 및 링크 저장
        zoomMeetingService.createMeetingAndSave(savedConsultation.getId());

        log.info("관리자 결제 확인 및 상담 생성 완료 - reservationId: {}, consultationId: {}",
                reservationId, savedConsultation.getId());

        // TODO: 주석 풀기
//        // Slack 알림 전송
//        String memberName = reservation.getGeneralProfile().getUser().getNickname();
//        String expertName = reservation.getExpertProfile().getUser().getNickname();
//        Integer price = reservation.getPrice();
//        String consultationType = reservation.getConsultationType().name();
//        slackNotificationService.sendPaymentConfirmationNotification(
//                reservationId,
//                savedConsultation.getId(),
//                memberName,
//                expertName,
//                price,
//                consultationType
//        );

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
        
        // 모든 이미지 키를 하나의 리스트로 수집 및 검증
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

        // 이미지 키 형식 검증
        if (!allImageKeys.isEmpty()) {
            List<String> invalidImageKeys = validateImageKeysFormat(allImageKeys);
            if (!invalidImageKeys.isEmpty()) {
                log.error("잘못된 이미지 키 형식 발견 - 저장 중단 - reservationId: {}, invalidKeys: {}",
                    reservationId, invalidImageKeys);
                throw new GlobalException(ReservationErrorCode.INVALID_IMAGE_KEY_FORMAT);
            }
        }
        
        FashionConcernJsonDTO fashionConcern = FashionConcernJsonDTO.builder()
                .type(Category.FASHION.name())
                .fashion(requestDTO.getFashion())
                .build();
        
        log.info("패션 상담 고민지 저장 - reservationId: {}, imageCount: {}", reservationId, allImageKeys.size());

        // JSON 문자열로 변환
        try {
            String concernJsonString = objectMapper.writeValueAsString(fashionConcern);
            reservation.updateConcerns(concernJsonString);
            log.info("패션 상담 고민지 업데이트 완료 - reservationId: {}, totalImages: {}", reservationId, allImageKeys.size());
        } catch (Exception e) {
            log.error("고민지 JSON 변환 실패 - reservationId: {}", reservationId, e);
            throw new GlobalException(ReservationErrorCode.CONCERN_JSON_CONVERSION_ERROR);
        }

        // TODO: 주석 풀기
//        // Slack 알림 전송
//        String username = reservation.getGeneralProfile().getUser().getNickname();
//        String category = reservation.getCategory().name();
//        slackNotificationService.sendFashionConcernUpdateNotification(reservationId, username, category);

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
        
        // 모든 이미지 키를 하나의 리스트로 수집 및 검증
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

        // 이미지 키 형식 검증
        if (!allImageKeys.isEmpty()) {
            List<String> invalidImageKeys = validateImageKeysFormat(allImageKeys);
            if (!invalidImageKeys.isEmpty()) {
                log.error("잘못된 이미지 키 형식 발견 - 저장 중단 - reservationId: {}, invalidKeys: {}",
                    reservationId, invalidImageKeys);
                throw new GlobalException(ReservationErrorCode.INVALID_IMAGE_KEY_FORMAT);
            }
        }
        
        HairConcernJsonDTO hairConcern = HairConcernJsonDTO.builder()
                .type(Category.HAIR.name())
                .hair(requestDTO.getHair())
                .build();
        
        log.info("헤어 상담 고민지 저장 - reservationId: {}, imageCount: {}", reservationId, allImageKeys.size());

        // JSON 문자열로 변환
        try {
            String concernJsonString = objectMapper.writeValueAsString(hairConcern);
            reservation.updateConcerns(concernJsonString);
            log.info("헤어 상담 고민지 업데이트 완료 - reservationId: {}, totalImages: {}", reservationId, allImageKeys.size());
        } catch (Exception e) {
            log.error("고민지 JSON 변환 실패 - reservationId: {}", reservationId, e);
            throw new GlobalException(ReservationErrorCode.CONCERN_JSON_CONVERSION_ERROR);
        }

        // TODO: 주석 풀기
//        // Slack 알림 전송
//        String username = reservation.getGeneralProfile().getUser().getNickname();
//        String category = reservation.getCategory().name();
//        slackNotificationService.sendHairConcernUpdateNotification(reservationId, username, category);

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
     * 기대 형식: tmp/consultation/reservation-{reservationId}/{imageType}/{fileName}
     * 예: tmp/consultation/reservation-1020/purpose/1.jpg
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
     * tmp/consultation/reservation-{reservationId}/{imageType}/{fileName}
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
            Long reservationId = reservation.getId();
            String expectedReservationPrefix = String.format("reservation-%d", reservationId);

            // 사전 검증: 모든 이미지 키가 유효한 형식이고 현재 예약에 속하는지 확인 (이동 작업 전)
            List<String> invalidImageKeys = validateImageKeysFormat(imageKeys);
            if (!invalidImageKeys.isEmpty()) {
                log.error("잘못된 이미지 키 형식 발견 - 이동 작업 중단 - reservationId: {}, consultationId: {}, invalidKeys: {}",
                    reservationId, consultationId, invalidImageKeys);
                throw new GlobalException(ReservationErrorCode.INVALID_IMAGE_KEY_FORMAT);
            }

            // 리소스 소유권 검증: 모든 이미지 키가 현재 예약의 reservation ID와 일치하는지 확인 (보안)
            // 이중 방어: 입력 단계에서도 검증했지만, 이동 단계에서도 반드시 강제
            List<String> unauthorizedImageKeys = new ArrayList<>();
            for (String imageKey : imageKeys) {
                String[] parts = imageKey.split("/");
                // 정확한 형식: tmp/consultation/reservation-{id}/{imageType}/{fileName}
                // parts[0]=tmp, parts[1]=consultation, parts[2]=reservation-{id}, parts[3]={imageType}, parts[4]={fileName}
                if (parts.length < 5) {
                    unauthorizedImageKeys.add(imageKey);
                    log.error("부족한 경로 구성요소 - 예상 5개, 실제: {}, imageKey: {}", parts.length, imageKey);
                    continue;
                }

                String reservationIdPart = parts[2];
                // 정확한 일치 검증 (equals 사용) - startsWith는 불안전
                if (!reservationIdPart.equals(expectedReservationPrefix)) {
                    unauthorizedImageKeys.add(imageKey);
                    log.warn("다른 예약의 이미지 키 감지 (데이터 유출 시도?) - " +
                        "reservationId: {}, consultationId: {}, expected: {}, actual: {}, imageKey: {}",
                        reservationId, consultationId, expectedReservationPrefix, reservationIdPart, imageKey);
                }
            }

            if (!unauthorizedImageKeys.isEmpty()) {
                log.error("권한 없는 이미지 키 발견 - 이동 작업 중단 (보안) - " +
                    "reservationId: {}, consultationId: {}, unauthorizedKeys: {}",
                    reservationId, consultationId, unauthorizedImageKeys);
                throw new GlobalException(ReservationErrorCode.INVALID_IMAGE_KEY_FORMAT);
            }

            // 모든 키가 유효하고 소유권이 확인된 경우에만 이미지 이동 시작
            for (String imageKey : imageKeys) {
                String[] parts = imageKey.split("/");

                if (parts.length < 5) {
                    log.error("예상치 못한 이미지 키 형식 - reservationId: {}, consultationId: {}, imageKey: {}",
                        reservationId, consultationId, imageKey);
                    throw new GlobalException(ReservationErrorCode.INVALID_IMAGE_KEY_FORMAT);
                }

                String imageType = parts[3];
                String fileName = parts[4];

                String finalS3Key = String.format("final/consultation/%d/%s/%s", 
                    consultationId, imageType, fileName);

                s3PresignedUrlService.moveImageFromTempToFinal(imageKey, finalS3Key);

                log.info("이미지 이동 완료 - reservationId: {}, from: {}, to: {}",
                    reservationId, imageKey, finalS3Key);
            }

            // ConcernJsonDTO의 imageKeys를 최종 경로로 업데이트
            // 모든 이미지 키는 이미 검증되었으므로 안전하게 변환
            // parts.length >= 5 보장됨 (위에서 < 5 체크하고 예외 발생)
            List<String> finalImageKeys = imageKeys.stream()
                    .map(imageKey -> {
                        String[] parts = imageKey.split("/");
                        // parts = [tmp, consultation, reservation-{id}, {imageType}, {fileName}]
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

        } catch (GlobalException e) {
            // GlobalException은 그대로 rethrow (의도적인 예외 유지)
            log.error("S3 이미지 이동 중 검증 오류 - consultationId: {}, resultCode: {}",
                consultation.getId(), e.getResultCode(), e);
            throw e;
        } catch (Exception e) {
            // JSON 처리 오류 등 예상치 못한 예외만 여기서 처리
            log.error("S3 이미지 이동 중 예상치 못한 오류 발생 - consultationId: {}",
                consultation.getId(), e);
            throw new GlobalException(ReservationErrorCode.CONCERN_JSON_CONVERSION_ERROR);
        }
    }

    /**
     * 상담 확정 시 채팅방 자동 생성 및 고민지 전송
     */
    @Transactional
    public void createChatroomsAndSendConcern(Consultation consultation, Reservation reservation, Long adminUserId) {
        log.info("채팅방 자동 생성 시작 - consultationId: {}, type: {}",
                consultation.getId(), consultation.getType());

        Long expertId = consultation.getExpertProfile().getUser().getId();
        Long memberId = consultation.getGeneralProfile().getUser().getId();
        Long consultationId = consultation.getId();
        String memberNickname = consultation.getGeneralProfile().getUser().getNickname();

        // 상담 타입에 따라 채팅방 생성
        ChatroomType chatroomType;
        if (consultation.getType() == ConsultationType.MESSAGE) {
            chatroomType = ChatroomType.MESSAGE;
        } else if (consultation.getType() == ConsultationType.VIDEO) {
            chatroomType = ChatroomType.VIDEO;
        } else {
            throw new GlobalException(ReservationErrorCode.INVALID_CONSULTATION_TYPE);
        }
        // 채팅방 생성
        Long chatroomId = createChatroom(expertId, consultationId, chatroomType);

        // 메시지 내용 구성
        String concernContent = String.format("%s님을 위한 고민지가 도착했습니다.", memberNickname);

        // 고민지가 있으면 자동 전송
        if (reservation.getConcernsJson() != null && !reservation.getConcernsJson().isEmpty()) {
            chatMessageService.sendConcernMessage(
                    chatroomId,
                    memberId,
                    consultationId,
                    concernContent
            );
            log.info("고민지 자동 전송 완료 - chatroomId: {}, consultationId: {}",
                    chatroomId, reservation.getId());
        }

        // 관리자 시스템 메시지 전송 (전문가에게 알림)
        Long adminChatroomId = createOrGetAdminChatroom(adminUserId, expertId);

        // 관리자-전문가 채팅방에 알림 전송
        String memberName = consultation.getGeneralProfile().getUser().getNickname();
        LocalDateTime scheduledDateTime = reservation.getScheduledDateTime();

        chatMessageService.sendReservationNotificationToExpert(
                adminChatroomId,
                adminUserId,
                memberName,
                scheduledDateTime,
                consultation.getType(),
                consultationId
        );
        log.info("관리자 시스템 메시지 전송 완료 - chatroomId: {}", chatroomId);

        log.info("채팅방 자동 생성 및 고민지 전송 완료 - consultationId: {}, chatroomId: {}",
                consultationId, chatroomId);
    }

    /**
     * 예약 주문서(결제 전 확인 페이지) 데이터 조회
     */
    public ReservationSheetResponseDTO getReservationSheet(Long userId, Long expertId, ConsultationType type) {

        // 예약자(User - Payer) 조회 및 포인트 확인
        User payer = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        if (payer.getGeneralProfile() == null) {
            throw new GlobalException(UserErrorCode.USER_NOT_FOUND);
        }

        // GeneralProfile.totalPoints는 Integer이므로 Long으로 변환
        Long currentPoints = Long.valueOf(payer.getGeneralProfile().getTotalPoints());


        // 전문가(Expert) 및 프로필 조회
        User expertUser = userRepository.findById(expertId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        if (!expertUser.isExpert()) {
            throw new GlobalException(ExpertErrorCode.USER_NOT_EXPERT);
        }
        ExpertProfile expertProfile = expertUser.getExpertProfile();


        // 전문가 계좌 정보 조회
        ExpertBankAccount bankAccount = expertRepository.findBankAccountByExpertProfileId(expertProfile.getId())
                .orElseThrow(() -> new GlobalException(ExpertErrorCode.NO_EXPERT_BANK_ACCOUNT));


        // 가격 책정 로직
        ConsultationSchedule schedule = consultationScheduleRepository
                .findByExpertProfileIdAndConsultationTypeAndIsActiveTrue(expertProfile.getId(), type)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.CONSULTATION_TYPE_NOT_SUPPORTED));

        Long price = Long.valueOf(schedule.getPrice());


        // DTO 조립 및 반환
        return ReservationSheetResponseDTO.builder()
                .targetInfo(ReservationSheetResponseDTO.ReservationTargetInfo.builder()
                        .expertNickname(expertUser.getNickname())
                        .category(expertProfile.getCategory())
                        .consultationType(type)
                        .originalPrice(price) // 조회한 스케줄의 가격
                        .build())
                .payerInfo(ReservationSheetResponseDTO.PayerInfo.builder()
                        .userNickname(payer.getNickname())
                        .totalPoints(currentPoints)
                        .build())
                .accountInfo(ReservationSheetResponseDTO.PaymentAccountInfo.builder()
                        .bankName(bankAccount.getBankName())
                        .accountNumber(bankAccount.getAccountNumber())
                        .accountHolder(bankAccount.getAccountHolder())
                        .build())
                .build();
    }

    /**
     * 관리자-전문가 알림 채팅방 생성 또는 조회
     */
    private Long createOrGetAdminChatroom(Long adminUserId, Long expertId) {
        log.info("관리자-전문가 채팅방 조회 시작 - adminId: {}, expertId: {}", adminUserId, expertId);

        // 관리자 User 조회
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        if (adminUser.getUserType() != UserType.ADMIN) {
            throw new GlobalException(UserErrorCode.ADMIN_PERMISSION_REQUIRED);
        }

        // 전문가 User 조회
        User expertUser = userRepository.findById(expertId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        // 기존 관리자-전문가 채팅방이 있는지 확인
        Optional<Chatroom> existingAdminChatroom = chatroomRepository
                .findActiveAdminChatroomByAdminAndExpert(adminUserId, expertId);

        if (existingAdminChatroom.isPresent()) {
            log.info("기존 관리자-전문가 채팅방 사용 - chatroomId: {}",
                    existingAdminChatroom.get().getId());
            return existingAdminChatroom.get().getId();
        }

        // 새로운 관리자-전문가 채팅방 생성
        Chatroom adminChatroom = Chatroom.builder()
                .consultationId(null)
                .chatroomType(ChatroomType.ADMIN)
                .member(adminUser)
                .expert(expertUser)
                .build();

        Chatroom savedAdminChatroom = chatroomRepository.save(adminChatroom);

        log.info("새 관리자-전문가 채팅방 생성 완료 - chatroomId: {}", savedAdminChatroom.getId());

        return savedAdminChatroom.getId();
    }

    /**
     * 특정 타입의 채팅방 생성
     * @return 생성된 채팅방 ID
     */
    private Long createChatroom(Long expertId, Long consultationId, ChatroomType chatroomType) {
        ChatroomCreateRequestDTO request = ChatroomCreateRequestDTO.builder()
                .consultationId(consultationId)
                .chatroomType(chatroomType)
                .build();

        ChatroomResponseDTO chatroom = chatroomService.createChatroom(expertId, consultationId, request);

        log.info("채팅방 생성 성공 - chatroomId: {}, type: {}",
                chatroom.getChatroomId(), chatroomType);

        return chatroom.getChatroomId();
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

    /**
     * 일반 사용자: 예약 취소 (UNPAID 또는 SUBMITTED)
     *
     * 취소 가능한 상태:
     * - UNPAID (임시 예약) - 작성 중인 예약
     * - SUBMITTED (제출 완료) - 입금 대기 중인 예약
     *
     * 권한:
     * - 예약한 본인만 취소 가능
     *
     * 수행 작업:
     * - 예약 상태를 CANCELLED로 변경
     *
     * 동시성 안전성:
     * - PESSIMISTIC_WRITE 락으로 동일 reservationId에 대한 동시 취소를 직렬화
     */
    @Transactional
    public void cancelTempReservation(Long reservationId, Long userId) {
        log.info("예약 취소 시작 - reservationId: {}, userId: {}", reservationId, userId);

        // PESSIMISTIC_WRITE 락을 사용한 예약 조회
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        // 예약 소유자 검증 (예약한 일반 회원과 현재 사용자가 동일한지 확인)
        if (!reservation.getGeneralProfile().getUser().getId().equals(userId)) {
            log.warn("권한 없음 - 예약 소유자가 아님 - reservationId: {}, userId: {}", reservationId, userId);
            throw new GlobalException(ReservationErrorCode.UNAUTHORIZED_RESERVATION_ACCESS);
        }

        // 예약 상태 검증 (UNPAID 또는 SUBMITTED 상태만 취소 가능)
        if (reservation.getReservationStatus() != ReservationStatus.UNPAID
                && reservation.getReservationStatus() != ReservationStatus.SUBMITTED) {
            log.warn("취소 불가능한 상태 - reservationId: {}, status: {}",
                    reservationId, reservation.getReservationStatus());
            throw new GlobalException(ReservationErrorCode.INVALID_RESERVATION_STATUS);
        }

        // 예약 상태를 CANCELLED로 변경
        reservation.cancel();
        reservationRepository.save(reservation);
        log.info("예약 취소 완료 - reservationId: {}, status: CANCELLED", reservationId);
    }

    /**
     * 일반 사용자: 결제 완료 예약(PAID) 환불 요청
     *
     * 환불 요청 가능한 상태:
     * - PAID (결제 완료된 예약)
     *
     * 권한:
     * - 예약한 본인만 환불 요청 가능
     *
     * 수행 작업:
     * - 예약 상태를 REFUND_REQUESTED로 변경
     *
     * 동시성 안전성:
     * - PESSIMISTIC_WRITE 락으로 동일 reservationId에 대한 동시 요청을 직렬화
     */
    @Transactional
    public void requestRefund(Long reservationId, Long userId) {
        log.info("환불 요청 시작 - reservationId: {}, userId: {}", reservationId, userId);

        // PESSIMISTIC_WRITE 락을 사용한 예약 조회
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        // 예약 소유자 검증 (예약한 일반 회원과 현재 사용자가 동일한지 확인)
        if (!reservation.getGeneralProfile().getUser().getId().equals(userId)) {
            log.warn("권한 없음 - 예약 소유자가 아님 - reservationId: {}, userId: {}", reservationId, userId);
            throw new GlobalException(ReservationErrorCode.UNAUTHORIZED_RESERVATION_ACCESS);
        }

        // 예약 상태 검증 (PAID 상태만 환불 요청 가능)
        if (reservation.getReservationStatus() != ReservationStatus.PAID) {
            log.warn("결제 완료 상태가 아님 - reservationId: {}, status: {}",
                    reservationId, reservation.getReservationStatus());
            throw new GlobalException(ReservationErrorCode.INVALID_RESERVATION_STATUS);
        }

        // 예약 상태를 REFUND_REQUESTED로 변경
        reservation.requestRefund();
        reservationRepository.save(reservation);
        log.info("환불 요청 완료 - reservationId: {}, status: REFUND_REQUESTED", reservationId);

        // TODO: 운영 시 주석 풀기
//        // Slack 알림 전송
//        String username = reservation.getGeneralProfile().getUser().getNickname();
//        Integer price = reservation.getPrice();
//        slackNotificationService.sendRefundRequestNotification(reservationId, username, price);
    }

    /**
     * 관리자: 환불 승인 처리
     *
     * 환불 승인 가능한 상태:
     * - REFUND_REQUESTED (사용자가 환불 요청한 상태)
     *
     * 권한:
     * - 관리자만 승인 가능
     *
     * 수행 작업:
     * - 예약 상태를 REFUNDED로 변경
     * - Consultation 상태를 REJECTED로 변경
     * - Consultation과 연결된 Chatroom을 비활성화 (isActive = false)
     *
     * 동시성 안전성:
     * - PESSIMISTIC_WRITE 락으로 동일 reservationId에 대한 동시 승인을 직렬화
     */
    @Transactional
    public void cancelPaidReservation(Long reservationId, Long adminUserId) {
        log.info("환불 승인 처리 시작 - reservationId: {}, adminUserId: {}", reservationId, adminUserId);

        // PESSIMISTIC_WRITE 락을 사용한 예약 조회
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        // 예약 상태 검증 (REFUND_REQUESTED 상태만 환불 승인 가능)
        if (reservation.getReservationStatus() != ReservationStatus.REFUND_REQUESTED) {
            log.warn("환불 요청 상태가 아님 - reservationId: {}, status: {}",
                    reservationId, reservation.getReservationStatus());
            throw new GlobalException(ReservationErrorCode.INVALID_RESERVATION_STATUS);
        }

        // 예약 상태를 REFUNDED로 변경
        reservation.refund();
        reservationRepository.save(reservation);
        log.info("예약 상태 변경 완료 - reservationId: {}, status: REFUNDED", reservationId);

        // 포인트 원상복구
        Integer pointsToRestore = reservation.getPointsToUse();
        if (pointsToRestore != null && pointsToRestore > 0) {
            GeneralProfile generalProfile = reservation.getGeneralProfile();
            User user = generalProfile.getUser();

            // 포인트 복구
            generalProfile.addPoints(pointsToRestore);

            // 포인트 히스토리 기록 (양수로 저장)
            PointHistory pointHistory = PointHistory.builder()
                    .user(user)
                    .point(pointsToRestore)  // 환불은 양수로 기록
                    .description("포인트 환불")
                    .build();
            pointHistoryRepository.save(pointHistory);

            log.info("포인트 원상복구 완료 - reservationId: {}, restoredPoints: {}, newTotalPoints: {}",
                    reservationId, pointsToRestore, generalProfile.getTotalPoints());
        } else {
            log.info("복구할 포인트 없음 - reservationId: {}, pointsToUse: {}", reservationId, pointsToRestore);
        }

        // Consultation 처리 (REFUND_REQUESTED 상태는 항상 Consultation이 존재함)
        Consultation consultation = reservation.getConsultation();
        if (consultation == null) {
            log.error("데이터 무결성 오류 - REFUND_REQUESTED 상태인데 Consultation이 없음 - reservationId: {}", reservationId);
            throw new GlobalException(ConsultationErrorCode.CONSULTATION_NOT_FOUND);
        }

        log.info("상담 거절 처리 시작 - consultationId: {}", consultation.getId());

        // Consultation 상태를 REJECTED로 변경
        consultation.reject();
        consultationRepository.save(consultation);
        log.info("상담 상태 변경 완료 - consultationId: {}, status: REJECTED", consultation.getId());

        // Consultation과 연결된 채팅방 비활성화
        List<Chatroom> chatrooms = chatroomRepository.findByConsultationId(consultation.getId());
        for (Chatroom chatroom : chatrooms) {
            if (chatroom.isActive()) {
                chatroom.deactivate();
                chatroomRepository.save(chatroom);
                log.info("채팅방 비활성화 완료 - chatroomId: {}, consultationId: {}",
                        chatroom.getId(), consultation.getId());
            }
        }

        log.info("환불 승인 처리 완료 - reservationId: {}, consultationId: {}, 비활성화된 채팅방 수: {}",
                reservationId, consultation.getId(), chatrooms.size());

        // TODO: 주석 풀기
//        // Slack 알림 전송
//        String memberName = reservation.getGeneralProfile().getUser().getNickname();
//        Integer price = reservation.getPrice();
//        slackNotificationService.sendRefundApprovalNotification(
//                reservationId,
//                consultation.getId(),
//                memberName,
//                price
//        );
    }

    /**
     * 포인트 적용/변경
     *
     * 사용자가 예약에 포인트를 적용하거나 변경할 수 있습니다.
     * - 전액 사용: pointsToUse = availablePoints
     * - 부분 사용: pointsToUse = 원하는 포인트
     * - 사용 취소: pointsToUse = 0
     *
     * @param reservationId 예약 ID
     * @param userId 사용자 ID
     * @param pointsToUse 사용할 포인트
     * @return 포인트 적용 결과
     */
    @Transactional
    public com.ceos.menual.domain.reservation.dto.response.PointApplicationResponseDTO applyPoints(
            Long reservationId,
            Long userId,
            Integer pointsToUse
    ) {
        log.info("포인트 적용 시작 - reservationId: {}, userId: {}, pointsToUse: {}",
                reservationId, userId, pointsToUse);

        // 예약 조회
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        // 권한 검증 (예약한 본인만 포인트 사용 가능)
        if (!reservation.getGeneralProfile().getUser().getId().equals(userId)) {
            log.warn("권한 없음 - 예약 소유자가 아님 - reservationId: {}, userId: {}",
                    reservationId, userId);
            throw new GlobalException(ReservationErrorCode.UNAUTHORIZED_RESERVATION_ACCESS);
        }

        // 상태 검증 (UNPAID 상태만 포인트 적용 가능)
        if (reservation.getReservationStatus() != ReservationStatus.UNPAID) {
            log.warn("임시 예약이 아님 - reservationId: {}, status: {}",
                    reservationId, reservation.getReservationStatus());
            throw new GlobalException(ReservationErrorCode.INVALID_RESERVATION_STATUS);
        }

        // 사용 가능한 포인트 계산
        Integer totalPoints = reservation.getGeneralProfile().getTotalPoints();
        if (totalPoints == null) {
            totalPoints = 0;
        }

        // 포인트 적용
        reservation.applyPoints(pointsToUse, totalPoints);
        reservationRepository.save(reservation);

        log.info("포인트 적용 완료 - reservationId: {}, pointsUsed: {}, finalPrice: {}",
                reservationId, reservation.getPointsToUse(), reservation.getFinalPrice());

        // 응답 생성
        return com.ceos.menual.domain.reservation.dto.response.PointApplicationResponseDTO.builder()
                .originalPrice(reservation.getPrice())
                .pointsUsed(reservation.getPointsToUse())
                .finalPrice(reservation.getFinalPrice())
                .remainingPoints(totalPoints - reservation.getPointsToUse())
                .build();
    }

    /**
     * Reservation Sheet 제출
     *
     * 사용자가 예약 주문서(고민지, 포인트 등)를 모두 작성한 후 제출합니다.
     * - UNPAID → SUBMITTED로 상태 변경
     * - 고민지 작성 여부 검증 (필수)
     * - 제출 후에는 포인트 변경 불가
     *
     * @param reservationId 예약 ID
     * @param userId 사용자 ID
     */
    @Transactional
    public void submitReservation(Long reservationId, Long userId) {
        log.info("예약 주문서 제출 시작 - reservationId: {}, userId: {}", reservationId, userId);

        // 예약 조회
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        // 권한 검증 (예약한 본인만 제출 가능)
        if (!reservation.getGeneralProfile().getUser().getId().equals(userId)) {
            log.warn("권한 없음 - 예약 소유자가 아님 - reservationId: {}, userId: {}",
                    reservationId, userId);
            throw new GlobalException(ReservationErrorCode.UNAUTHORIZED_RESERVATION_ACCESS);
        }

        // 상태 검증 (UNPAID 상태만 제출 가능)
        if (reservation.getReservationStatus() != ReservationStatus.UNPAID) {
            log.warn("임시 예약이 아님 - reservationId: {}, status: {}",
                    reservationId, reservation.getReservationStatus());
            throw new GlobalException(ReservationErrorCode.INVALID_RESERVATION_STATUS);
        }

        // TODO: 개발 단계에서는 주석 처리
//        // 고민지 작성 여부 검증
//        if (reservation.getConcernsJson() == null || reservation.getConcernsJson().isEmpty()) {
//            log.warn("고민지 미작성 - reservationId: {}", reservationId);
//            throw new GlobalException(ReservationErrorCode.MISSING_CONCERN_DATA);
//        }

        // finalPrice 설정 (포인트를 사용하지 않은 경우)
        if (reservation.getFinalPrice() == null) {
            reservation.applyPoints(0, 0); // finalPrice = price - 0
        }

        // SUBMITTED로 변경
        reservation.submit();
        reservationRepository.save(reservation);

        log.info("예약 주문서 제출 완료 - reservationId: {}, status: SUBMITTED", reservationId);

        // Slack 알림 전송
        // TODO: 프로덕션 배포 시 주석 해제
        /*
        slackNotificationService.sendReservationSubmittedNotification(
                reservation.getId(),
                reservation.getGeneralProfile().getUser().getUsername(),
                reservation.getCategory().name(),
                reservation.getConsultationType().name(),
                reservation.getFinalPrice()
        );
        */
    }

    /**
     * 결제 내역 조회 (전체)
     *
     * 사용자의 모든 결제 완료된 예약 내역을 조회합니다.
     * - PAID 상태인 예약만 조회
     * - 최신순 정렬 (updatedAt 기준)
     *
     * @param userId 사용자 ID
     * @return 결제 내역 리스트
     */
    @Transactional(readOnly = true)
    public com.ceos.menual.domain.reservation.dto.response.PaymentHistoryListResponseDTO getPaymentHistory(Long userId) {
        log.info("결제 내역 조회 시작 - userId: {}", userId);

        // PAID 상태인 예약 조회
        List<Reservation> paidReservations = reservationRepository.findPaymentHistoryByUserId(
                userId,
                ReservationStatus.PAID
        );

        // DTO 변환
        List<com.ceos.menual.domain.reservation.dto.response.PaymentHistoryResponseDTO> historyDTOs =
                paidReservations.stream()
                        .map(com.ceos.menual.domain.reservation.dto.response.PaymentHistoryResponseDTO::from)
                        .toList();

        log.info("결제 내역 조회 완료 - userId: {}, count: {}", userId, historyDTOs.size());

        return com.ceos.menual.domain.reservation.dto.response.PaymentHistoryListResponseDTO.builder()
                .listCount(historyDTOs.size())
                .payments(historyDTOs)
                .build();
    }

    /**
     * 결제 내역 조회 (카테고리별)
     *
     * 사용자의 특정 카테고리 결제 완료된 예약 내역을 조회합니다.
     * - PAID 상태이고 특정 카테고리인 예약만 조회
     * - 최신순 정렬 (updatedAt 기준)
     *
     * @param userId 사용자 ID
     * @param category 카테고리 (HAIR, FASHION, SKIN, MAKEUP)
     * @return 결제 내역 리스트
     */
    @Transactional(readOnly = true)
    public com.ceos.menual.domain.reservation.dto.response.PaymentHistoryListResponseDTO getPaymentHistoryByCategory(
            Long userId,
            Category category
    ) {
        log.info("카테고리별 결제 내역 조회 시작 - userId: {}, category: {}", userId, category);

        // PAID 상태이고 특정 카테고리인 예약 조회
        List<Reservation> paidReservations = reservationRepository.findPaymentHistoryByUserIdAndCategory(
                userId,
                ReservationStatus.PAID,
                category
        );

        // DTO 변환
        List<com.ceos.menual.domain.reservation.dto.response.PaymentHistoryResponseDTO> historyDTOs =
                paidReservations.stream()
                        .map(com.ceos.menual.domain.reservation.dto.response.PaymentHistoryResponseDTO::from)
                        .toList();

        log.info("카테고리별 결제 내역 조회 완료 - userId: {}, category: {}, count: {}",
                userId, category, historyDTOs.size());

        return com.ceos.menual.domain.reservation.dto.response.PaymentHistoryListResponseDTO.builder()
                .listCount(historyDTOs.size())
                .payments(historyDTOs)
                .build();
    }

}