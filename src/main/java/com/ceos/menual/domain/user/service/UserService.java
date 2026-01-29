package com.ceos.menual.domain.user.service;

import com.ceos.menual.domain.consultation.repository.ConsultationRepository;
import com.ceos.menual.domain.email.exception.EmailErrorCode;
import com.ceos.menual.domain.email.service.EmailVerificationService;
import com.ceos.menual.domain.expert.repository.ExpertLikeRepository;
import com.ceos.menual.domain.reservation.repository.ReservationRepository;
import com.ceos.menual.domain.review.repository.ReviewRepository;
import com.ceos.menual.domain.user.dto.request.ExpertConversionRequestDTO;
import com.ceos.menual.domain.user.dto.request.SignUpRequestDTO;
import com.ceos.menual.domain.user.dto.request.SocialSignUpRequestDTO;
import com.ceos.menual.domain.user.dto.response.ExpertConversionResponseDTO;
import com.ceos.menual.domain.user.dto.response.SignUpResponseDTO;
import com.ceos.menual.domain.user.dto.response.SocialSignUpResponseDTO;
import com.ceos.menual.domain.user.dto.response.UserInfoResponseDTO;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.ExpertProfileRepository;
import com.ceos.menual.domain.user.repository.GeneralProfileRepository;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.*;
import com.ceos.menual.entity.enums.AuthProvider;

import com.ceos.menual.entity.enums.UserType;
import com.ceos.menual.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ExpertLikeRepository expertLikeRepository;
    private final ReviewRepository reviewRepository;
    private final GeneralProfileRepository generalProfileRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final com.ceos.menual.domain.user.repository.PointHistoryRepository pointHistoryRepository;
    private final ReservationRepository reservationRepository;
    private final ConsultationRepository consultationRepository;
    private final EmailVerificationService emailVerificationService;
    private final com.ceos.menual.global.config.slack.SlackNotificationService slackNotificationService;

    @Transactional
    public SignUpResponseDTO signUp(SignUpRequestDTO request) {
        log.info("회원가입 시작 - email: {}, nickname: {}", request.getEmail(), request.getNickname());

        // 이메일 인증 여부 확인
        if (!emailVerificationService.isEmailVerified(request.getEmail())) {
            log.warn("회원가입 실패 - 이메일 미인증 - email: {}", request.getEmail());
            throw new GlobalException(EmailErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 비밀번호 일치 검증
        if (!request.isPasswordMatch()) {
            log.warn("회원가입 실패 - 비밀번호 불일치 - email: {}", request.getEmail());
            throw new GlobalException(UserErrorCode.INVALID_PASSWORD);
        }

        // 이메일 중복 검사
        validateDuplicateEmail(request.getEmail());

        // 닉네임 중복 검사
        validateDuplicateNickname(request.getNickname());

        // User 엔티티 생성
        User user = User.builder()
                .nickname(request.getNickname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .birth(parseBirthDate(request.getBirth()))
                .userType(UserType.MEMBER)
                .provider(AuthProvider.LOCAL)
                .agreeTerms(request.getAgreeTerms())
                .agreePrivacy(request.getAgreePrivacy())
                .build();

        // 저장
        User savedUser = userRepository.save(user);
        log.info("User 저장 완료 - userId: {}, email: {}", savedUser.getId(), savedUser.getEmail());

        // GeneralProfile 생성
        GeneralProfile generalProfile = GeneralProfile.builder()
                .user(savedUser)
                .totalPoints(0)
                .build();

        generalProfileRepository.save(generalProfile);
        log.info("GeneralProfile 생성 완료 - userId: {}", savedUser.getId());

        // 이메일 인증 상태 삭제
        emailVerificationService.clearVerifiedStatus(request.getEmail());

        log.info("회원가입 완료 - userId: {}, email: {}, nickname: {}",
                savedUser.getId(), savedUser.getEmail(), savedUser.getNickname());

        // 응답 생성
        return SignUpResponseDTO.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .nickname(savedUser.getNickname())
                .userType(savedUser.getUserType())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    @Transactional
    public SocialSignUpResponseDTO socialSignUp(Long userId, SocialSignUpRequestDTO request) {
        log.info("소셜 회원가입 시작 - userId: {}, email: {}, nickname: {}",
                userId, request.getEmail(), request.getNickname());

        // 소셜 로그인 시 생성된 유저인지 검증
        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                log.warn("소셜 회원가입 실패 - 사용자 없음 - userId: {}", userId);
                return new GlobalException(UserErrorCode.USER_NOT_FOUND);
            });

        // 이메일 중복 검사
        validateDuplicateEmail(request.getEmail());

        // 닉네임 중복 검사
        validateDuplicateNickname(request.getNickname());

        // 사용자 정보 업데이트
        user.updateSocialExtraInfo(
            request.getNickname(),
            parseBirthDate(request.getBirth()),
            request.getEmail(),
            request.getAgreeTerms(),
            request.getAgreePrivacy()
        );
        log.info("소셜 회원 정보 업데이트 완료 - userId: {}", userId);

        // GeneralProfile 생성 (소셜 로그인 추가 정보 입력 시점에 생성)
        if (user.getGeneralProfile() == null) {
            GeneralProfile generalProfile = GeneralProfile.builder()
                    .user(user)
                    .totalPoints(0)
                    .build();

            generalProfileRepository.save(generalProfile);
            log.info("GeneralProfile 생성 완료 - userId: {}", userId);
        }

        log.info("소셜 회원가입 완료 - userId: {}, email: {}, nickname: {}",
                userId, user.getEmail(), user.getNickname());

        // 저장 후 응답 반환
        return SocialSignUpResponseDTO.builder()
            .nickname(user.getNickname())
            .userType(user.getUserType())
            .build();
    }


    @Transactional
    public ExpertConversionResponseDTO convertToExpert(String email, ExpertConversionRequestDTO requestDTO) {
        log.info("전문가 전환 시작 - email: {}, category: {}", email, requestDTO.getCategory());

        // 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("전문가 전환 실패 - 사용자 없음 - email: {}", email);
                    return new GlobalException(UserErrorCode.USER_NOT_FOUND);
                });

        // 이미 전문가인지 확인
        if (user.getUserType() == UserType.EXPERT) {
            log.warn("전문가 전환 실패 - 이미 전문가 - email: {}", email);
            throw new GlobalException(UserErrorCode.USER_ALREADY_EXPERT);
        }

        // GeneralProfile은 유지 (과거 상담 기록 보존, FK 제약 해결)
        // 전문가의 예약은 ReservationService에서 UserType 체크로 차단

        // ExpertProfile 생성 및 저장
        ExpertProfile expertProfile = ExpertProfile.builder()
                .user(user)
                .category(requestDTO.getCategory())
                .specialities(requestDTO.getSpecialities() != null ? requestDTO.getSpecialities() : new ArrayList<>())
                .introduction(requestDTO.getIntroduction())
                .profileLink(requestDTO.getProfileLink())
                .careerInfo(requestDTO.getCareerInfo())
                .consultationSchedules(new ArrayList<>())
                .build();

        ExpertProfile savedExpertProfile = expertProfileRepository.save(expertProfile);

        // UserType 변경 + ExpertProfile 설정 + 닉네임 변경 (한 번에 처리)
        user.convertToExpert(savedExpertProfile);

        log.info("전문가 전환 완료 - userId: {}, nickname: {}, category: {}",
                user.getId(), user.getNickname(), savedExpertProfile.getCategory());

        // Slack 알림 전송
        slackNotificationService.sendExpertConversionNotification(
                user.getId(),
                user.getNickname(),
                savedExpertProfile.getCategory().name()
        );

        // 응답 생성
        return ExpertConversionResponseDTO.builder()
                .expertProfileId(savedExpertProfile.getId())
                .userType(user.getUserType())
                .category(savedExpertProfile.getCategory())
                .nickname(user.getNickname())
                .build();
    }

    // ============== 비즈니스 메서드 ============== //

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("회원가입 실패 - 이메일 중복 - email: {}", email);
            throw new GlobalException(UserErrorCode.DUPLICATE_EMAIL);
        }
    }

    private void validateDuplicateNickname(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            log.warn("회원가입 실패 - 닉네임 중복 - nickname: {}", nickname);
            throw new GlobalException(UserErrorCode.DUPLICATE_NICKNAME);
        }
    }

    // YYYYMMDD -> LocalDate 변환
    private LocalDate parseBirthDate(String birth) {
        try {
            String year = birth.substring(0, 4);
            String month = birth.substring(4, 6);
            String day = birth.substring(6, 8);
            return LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
        } catch (Exception e) {
            throw new GlobalException(UserErrorCode.INVALID_BIRTH_FORMAT);
        }
    }

    // 이메일 마스킹 (te**@example.com)
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    // 닉네임 마스킹 (홍**)
    private String maskNickname(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return "***";
        }
        if (nickname.length() == 1) {
            return nickname + "**";
        }
        return nickname.substring(0, 1) + "**";
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public UserInfoResponseDTO getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        Long expertLikeCount;
        Long reviewCount;

        // 전문가(EXPERT)인 경우
        if (user.getUserType() == UserType.EXPERT) {
            ExpertProfile ep = user.getExpertProfile();
            if (ep == null) {
                // 전문가 타입인데 프로필이 없는 비정상 상태
                throw new GlobalException(UserErrorCode.EXPERT_PROFILE_NOT_FOUND);
            }

            expertLikeCount = expertLikeRepository.countByExpertProfileId(ep.getId());
            reviewCount = reviewRepository.countByConsultationExpertProfileId(ep.getId());
        }
        // 일반 유저(MEMBER/GENERAL)인 경우
        else {
            GeneralProfile gp = user.getGeneralProfile();
            if (gp == null) {
                // 일반 유저인데 프로필이 없는 비정상 상태
                throw new GlobalException(UserErrorCode.GENERAL_PROFILE_NOT_FOUND);
            }

            expertLikeCount = expertLikeRepository.countByGeneralProfileId(gp.getId());
            reviewCount = reviewRepository.countByConsultationGeneralProfileId(gp.getId());
        }
        return UserInfoResponseDTO.of(user, expertLikeCount, reviewCount);
    }

    /**
     * 포인트 히스토리 조회
     *
     * 사용자의 포인트 적립 및 사용 내역을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 현재 보유 포인트와 포인트 히스토리 리스트
     */
    @Transactional(readOnly = true)
    public com.ceos.menual.domain.user.dto.response.PointHistoryListResponseDTO getPointHistory(Long userId) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        // GeneralProfile 조회
        GeneralProfile generalProfile = user.getGeneralProfile();
        if (generalProfile == null) {
            throw new GlobalException(UserErrorCode.GENERAL_PROFILE_NOT_FOUND);
        }

        // 현재 보유 포인트
        Integer totalPoints = generalProfile.getTotalPoints();
        if (totalPoints == null) {
            totalPoints = 0;
        }

        // 포인트 히스토리 조회 (최신순)
        List<PointHistory> pointHistories = pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // DTO 변환
        List<com.ceos.menual.domain.user.dto.response.PointHistoryResponseDTO> historyDTOs =
                pointHistories.stream()
                        .map(com.ceos.menual.domain.user.dto.response.PointHistoryResponseDTO::from)
                        .toList();

        return com.ceos.menual.domain.user.dto.response.PointHistoryListResponseDTO.builder()
                .totalPoints(totalPoints)
                .history(historyDTOs)
                .build();
    }

    /**
     * 회원 탈퇴
     * - 진행 중인 예약/상담이 있으면 탈퇴 불가
     * - Soft delete 방식으로 deletedAt 필드에 탈퇴 일시 저장
     * - PESSIMISTIC_WRITE 락으로 동시성 문제 해결 (탈퇴 처리 중 예약/상담 생성 방지)
     */
    @Transactional
    public void withdraw(Long userId) {
        // PESSIMISTIC_WRITE 락으로 User 조회 - 동시 예약/상담 생성 방지
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        // 이미 탈퇴한 회원인지 확인
        if (user.isWithdrawn()) {
            throw new GlobalException(UserErrorCode.USER_ALREADY_WITHDRAWN);
        }

        // 일반 회원인 경우 진행 중인 예약/상담 확인
        if (user.getGeneralProfile() != null) {
            Long generalProfileId = user.getGeneralProfile().getId();

            // 진행 중인 예약 확인
            if (reservationRepository.existsActiveReservationByGeneralProfileId(generalProfileId)) {
                throw new GlobalException(UserErrorCode.ACTIVE_RESERVATION_EXISTS);
            }

            // 진행 중인 상담 확인
            if (consultationRepository.existsActiveConsultationByGeneralProfileId(generalProfileId)) {
                throw new GlobalException(UserErrorCode.ACTIVE_CONSULTATION_EXISTS);
            }
        }

        // 전문가인 경우 진행 중인 예약/상담 확인
        if (user.getExpertProfile() != null) {
            Long expertProfileId = user.getExpertProfile().getId();

            // 진행 중인 예약 확인
            if (reservationRepository.existsActiveReservationByExpertProfileId(expertProfileId)) {
                throw new GlobalException(UserErrorCode.ACTIVE_RESERVATION_EXISTS);
            }

            // 진행 중인 상담 확인
            if (consultationRepository.existsActiveConsultationByExpertProfileId(expertProfileId)) {
                throw new GlobalException(UserErrorCode.ACTIVE_CONSULTATION_EXISTS);
            }
        }

        // 탈퇴 처리 (Soft delete)
        user.withdraw();
    }
}
