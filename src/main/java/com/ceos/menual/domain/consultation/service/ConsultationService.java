package com.ceos.menual.domain.consultation.service;

import com.ceos.menual.domain.consultation.dto.request.SolutionRequestDTO;
import com.ceos.menual.domain.consultation.dto.response.ConsultationHistoryResponseDTO;
import com.ceos.menual.domain.consultation.exception.ConsultationErrorCode;
import com.ceos.menual.domain.consultation.repository.ConsultationRepository;
import com.ceos.menual.domain.reservation.exception.ReservationErrorCode;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.Consultation;
import com.ceos.menual.entity.GeneralProfile;
import com.ceos.menual.entity.User;
import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultationService {

    private final UserRepository userRepository;
    private final ConsultationRepository consultationRepository;

    /**
     * 지난 상담 내역 조회 (전체)
     * - PAID + 과거 일정만 조회
     * - 카테고리별 필터링 가능
     */
    public List<ConsultationHistoryResponseDTO> getConsultationHistory(Long userId, Category category) {
        log.info("지난 상담 내역 조회 시작 - userId: {}, category: {}", userId, category);

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        // GeneralProfile 조회 (일반 회원만 조회 가능)
        if (user.getGeneralProfile() == null) {
            throw new GlobalException(ReservationErrorCode.GENERAL_PROFILE_NOT_FOUND);
        }

        GeneralProfile generalProfile = user.getGeneralProfile();

        // 지난 상담 내역 전체 조회 (QueryDSL)
        List<ConsultationHistoryResponseDTO> consultations =
                consultationRepository.findConsultationHistory(generalProfile.getId(), category);

        log.info("지난 상담 내역 조회 완료 - 조회된 개수: {}", consultations.size());

        return consultations;
    }

    /**
     * 솔루션 저장 - 해당 전문가만 가능
     */
    public Consultation saveSolution(Long consultationId, SolutionRequestDTO solutionRequestDTO, Long expertProfileId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new GlobalException(ConsultationErrorCode.CONSULTATION_NOT_FOUND));

        // 해당 전문가만 솔루션을 저장할 수 있음
        if (!consultation.getExpertProfile().getId().equals(expertProfileId)) {
            throw new GlobalException(ConsultationErrorCode.UNAUTHORIZED_CONSULTATION);
        }

        consultation.updateSolution(solutionRequestDTO.getSolution());
        return consultationRepository.save(consultation);
    }

    /**
     * 솔루션 조회 - 해당 전문가 또는 상담 회원만 가능
     */
    @Transactional(readOnly = true)
    public String getSolution(Long consultationId, Long userId, String userType) {
        Consultation consultation = consultationRepository.findByIdWithProfiles(consultationId)
                .orElseThrow(() -> new GlobalException(ConsultationErrorCode.CONSULTATION_NOT_FOUND));

        // Null 체크
        if (consultation.getExpertProfile() == null || consultation.getExpertProfile().getUser() == null) {
            log.error("상담 ID: {}의 전문가 프로필 또는 사용자가 없습니다", consultationId);
            throw new GlobalException(ConsultationErrorCode.CONSULTATION_NOT_FOUND);
        }

        if (consultation.getGeneralProfile() == null || consultation.getGeneralProfile().getUser() == null) {
            log.error("상담 ID: {}의 회원 프로필 또는 사용자가 없습니다", consultationId);
            throw new GlobalException(ConsultationErrorCode.CONSULTATION_NOT_FOUND);
        }

        Long expertUserId = consultation.getExpertProfile().getUser().getId();
        Long memberUserId = consultation.getGeneralProfile().getUser().getId();

        log.debug("권한 검증 - 요청 사용자 ID: {}, 타입: {}", userId, userType);
        log.debug("전문가 User ID: {}, 회원 User ID: {}", expertUserId, memberUserId);

        // 해당 전문가 또는 상담 회원만 조회 가능
        boolean isExpert = "EXPERT".equals(userType) && expertUserId.equals(userId);
        boolean isMember = "MEMBER".equals(userType) && memberUserId.equals(userId);

        log.debug("isExpert: {}, isMember: {}", isExpert, isMember);

        if (!isExpert && !isMember) {
            log.warn("권한 없음 - 사용자: {}, 타입: {}", userId, userType);
            throw new GlobalException(ConsultationErrorCode.UNAUTHORIZED_CONSULTATION);
        }

        return consultation.getSolution();
    }
}