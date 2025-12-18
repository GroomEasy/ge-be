package com.ceos.menual.domain.review.repository;

import java.util.List;

import com.ceos.menual.domain.review.dto.response.ReviewSummaryResponseDTO;

public interface ReviewRepository {
	List<ReviewSummaryResponseDTO> findRecentReviews(Long categoryId);

	List<ReviewSummaryResponseDTO> findBestReviews(Long categoryId);
}
