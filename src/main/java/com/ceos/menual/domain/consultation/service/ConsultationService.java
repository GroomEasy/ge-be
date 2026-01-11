package com.ceos.menual.domain.consultation.service;

import com.ceos.menual.entity.Consultation;
import com.ceos.menual.domain.consultation.dto.request.SolutionRequestDTO;
import com.ceos.menual.domain.consultation.repository.ConsultationRepository;
import com.ceos.menual.domain.consultation.exception.ConsultationErrorCode;
import com.ceos.menual.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationService {

    private final ConsultationRepository consultationRepository;

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
