package com.ceos.menual.domain.expert.repository;

import java.util.List;
import java.util.Optional;

import com.ceos.menual.domain.expert.dto.response.ExpertRankingResponseDTO;
import com.ceos.menual.domain.expert.dto.response.ExpertSummaryResponseDTO;
import com.ceos.menual.entity.ExpertLike;
import com.ceos.menual.entity.ExpertProfile;
import com.ceos.menual.entity.enums.Category;

public interface ExpertRepository {
	List<ExpertRankingResponseDTO> findTop3Overall();
	List<ExpertRankingResponseDTO> findTop3ByCategory(Category category, Long currentUserId);
	// 전문가 조회용
	List<ExpertSummaryResponseDTO> findExpertList(Category category, int page, int size);
}
