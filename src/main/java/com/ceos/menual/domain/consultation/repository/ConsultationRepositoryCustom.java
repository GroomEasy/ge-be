package com.ceos.menual.domain.consultation.repository;

import com.ceos.menual.domain.consultation.dto.response.ConsultationHistoryResponseDTO;
import com.ceos.menual.entity.enums.Category;

import java.util.List;

public interface ConsultationRepositoryCustom {

    /**
     * 지난 상담 내역 전체 조회
     * @param generalProfileId 일반 회원 프로필 ID
     * @param category 카테고리 (null이면 전체 조회)
     * @return 지난 상담 내역 리스트
     */
    List<ConsultationHistoryResponseDTO> findConsultationHistory(
            Long generalProfileId,
            Category category
    );
}