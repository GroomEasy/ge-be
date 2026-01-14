package com.ceos.menual.domain.expert.repository;

import com.ceos.menual.domain.expert.dto.response.ExpertSummaryResponseDTO;
import com.ceos.menual.entity.enums.Category;

import java.util.List;

public interface ExpertLikeRepositoryCustom {
    List<ExpertSummaryResponseDTO> findLikedExpertList(Long generalProfileId, Category category, int page, int size);
}