package com.ceos.menual.domain.expert.repository;

import java.util.List;

import com.ceos.menual.domain.expert.dto.response.ExpertRankingResponseDTO;
import com.ceos.menual.entity.enums.Category;

public interface ExpertRepository {
	List<ExpertRankingResponseDTO> findTop3Overall();
	List<ExpertRankingResponseDTO> findTop3ByCategory(Category category);
}
