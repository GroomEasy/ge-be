package com.ceos.menual.domain.consultation.service;

import com.ceos.menual.domain.consultation.dto.response.ConsultationHistoryResponseDTO;
import com.ceos.menual.domain.consultation.repository.ConsultationRepository;
import com.ceos.menual.domain.reservation.exception.ReservationErrorCode;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.UserRepository;
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
}