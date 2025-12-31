package com.ceos.menual.domain.review.repository;

import java.util.List;

import com.ceos.menual.domain.review.dto.response.ReviewSummaryResponseDTO;
import com.ceos.menual.entity.enums.Category;

public interface ReviewRepository {
	List<ReviewSummaryResponseDTO> findRecentReviews(Category category, int page, int size);

	List<ReviewSummaryResponseDTO> findBestReviews(Category category);
}
