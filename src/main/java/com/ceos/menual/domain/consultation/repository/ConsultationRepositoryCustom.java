package com.ceos.menual.domain.consultation.repository;

import com.ceos.menual.domain.consultation.dto.response.ConsultationHistoryResponseDTO;
import com.ceos.menual.domain.consultation.dto.response.ExpertConsultationHistoryResponseDTO;
import com.ceos.menual.domain.consultation.dto.response.SolutionListResponseDTO;
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

    /**
     * 솔루션이 작성된 상담 목록 조회
     * @param generalProfileId 일반 회원 프로필 ID
     * @param category 카테고리 (null이면 전체 조회)
     * @return 솔루션 목록 리스트
     */
    List<SolutionListResponseDTO> findSolutionList(
            Long generalProfileId,
            Category category
    );

    /**
     * 전문가의 상담 내역 조회
     * @param expertProfileId 전문가 프로필 ID
     * @return 전문가 상담 내역 리스트
     */
    List<ExpertConsultationHistoryResponseDTO> findExpertConsultationHistory(Long expertProfileId);
}