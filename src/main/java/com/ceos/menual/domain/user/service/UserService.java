package com.ceos.menual.domain.user.service;

import com.ceos.menual.domain.consultation.repository.ConsultationRepository;
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
import com.querydsl.core.QueryFactory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ExpertLikeRepository expertLikeRepository;
    private final ReviewRepository reviewRepository;
    private final GeneralProfileRepository generalProfileRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final com.ceos.menual.domain.user.repository.PointHistoryRepository pointHistoryRepository;
    private final ReservationRepository reservationRepository;
    private final ConsultationRepository consultationRepository;

    @Transactional
    public SignUpResponseDTO signUp(SignUpRequestDTO request) {
        // 비밀번호 일치 검증
        if (!request.isPasswordMatch()) {
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

        // GeneralProfile 생성
        GeneralProfile generalProfile = GeneralProfile.builder()
                .user(savedUser)
                .totalPoints(0)
                .build();

        generalProfileRepository.save(generalProfile);

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
        // 소셜 로그인 시 생성된 유저인지 검증
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

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

        // GeneralProfile 생성 (소셜 로그인 추가 정보 입력 시점에 생성)
        if (user.getGeneralProfile() == null) {
            GeneralProfile generalProfile = GeneralProfile.builder()
                    .user(user)
                    .totalPoints(0)
                    .build();

            generalProfileRepository.save(generalProfile);
        }

        // 저장 후 응답 반환
        return SocialSignUpResponseDTO.builder()
            .nickname(user.getNickname())
            .userType(user.getUserType())
            .build();
    }


    @Transactional
    public ExpertConversionResponseDTO convertToExpert(Long userId, ExpertConversionRequestDTO requestDTO) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        // 이미 전문가인지 확인
        if (user.getUserType() == UserType.EXPERT) {
            throw new GlobalException(UserErrorCode.USER_ALREADY_EXPERT);
        }

        // 계좌 정보 일부만 입력된 경우 예외 처리
        if (requestDTO.hasPartialBankAccountInfo()) {
            throw new GlobalException(UserErrorCode.INCOMPLETE_BANK_ACCOUNT_INFO);
        }

        // GeneralProfile 조회 및 연관 데이터 삭제
        GeneralProfile generalProfile = user.getGeneralProfile();
        if (generalProfile != null) {
            Long generalProfileId = generalProfile.getId();

            // 연관 데이터 삭제 (JPA 방식 사용)
            QReview review = QReview.review;
            QConsultation consultation = QConsultation.consultation;

            List<Review> reviews = queryFactory
                    .selectFrom(review)
                    .join(review.consultation, consultation).fetchJoin()
                    .where(consultation.generalProfile.id.eq(generalProfileId))
                    .fetch();

            reviews.forEach(entityManager::remove);

            // ExpertLike 삭제
            expertLikeRepository.deleteByGeneralProfileId(generalProfileId);

            // GeneralProfile 삭제
            generalProfileRepository.delete(generalProfile);
        }

        // ExpertProfile 생성 및 저장
        ExpertProfile.ExpertProfileBuilder expertProfileBuilder = ExpertProfile.builder()
                .user(user)
                .category(requestDTO.getCategory())
                .specialities(requestDTO.getSpecialities() != null ? requestDTO.getSpecialities() : new ArrayList<>())
                .introduction(requestDTO.getIntroduction())
                .profileLink(requestDTO.getProfileLink())
                .careerInfo(requestDTO.getCareerInfo())
                .consultationSchedules(new ArrayList<>());

        // ExpertBankAccount 생성 (정보가 완전히 입력된 경우에만)
        if (requestDTO.hasCompleteBankAccountInfo()) {
            ExpertBankAccount bankAccount = ExpertBankAccount.builder()
                    .bankName(requestDTO.getBankName())
                    .accountNumber(requestDTO.getAccountNumber())
                    .accountHolder(requestDTO.getAccountHolder())
                    .build();

            expertProfileBuilder.expertBankAccount(bankAccount);
        }

        ExpertProfile expertProfile = expertProfileBuilder.build();

        // ExpertBankAccount가 있으면 양방향 관계 설정
        if (expertProfile.getExpertBankAccount() != null) {
            expertProfile.getExpertBankAccount().setExpertProfile(expertProfile);
        }

        ExpertProfile savedExpertProfile = expertProfileRepository.save(expertProfile);

        // UserType 변경 + ExpertProfile 설정 (한 번에 처리)
        user.convertToExpert(savedExpertProfile);

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
            throw new GlobalException(UserErrorCode.DUPLICATE_EMAIL);
        }
    }

    private void validateDuplicateNickname(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
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
