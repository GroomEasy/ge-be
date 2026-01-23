package com.ceos.menual.domain.review.repository;

import java.util.List;

import com.ceos.menual.domain.review.dto.response.AvailableReviewResponseDTO;
import com.ceos.menual.domain.review.dto.response.CompletedReviewResponseDTO;
import com.ceos.menual.domain.review.dto.response.ReviewSummaryResponseDTO;
import com.ceos.menual.entity.enums.Category;

public interface ReviewRepository {
	List<ReviewSummaryResponseDTO> findRecentReviews(Category category, int page, int size);

	List<ReviewSummaryResponseDTO> findBestReviews(Category category);

	Long countByConsultationGeneralProfileId(Long generalProfileId);

	/**
	 * 작성 가능한 후기 목록 조회
	 * @param generalProfileId 일반 회원 프로필 ID
	 * @return 작성 가능한 후기 목록
	 */
	List<AvailableReviewResponseDTO> findAvailableReviews(Long generalProfileId);

	/**
	 * 작성 완료된 후기 목록 조회
	 * @param generalProfileId 일반 회원 프로필 ID
	 * @return 작성 완료된 후기 목록
	 */
	List<CompletedReviewResponseDTO> findCompletedReviews(Long generalProfileId);

	void deleteByConsultationGeneralProfileId(Long generalProfileId);

	Long countByConsultationExpertProfileId(Long expertProfileId);

	/**
	 * 특정 전문가의 후기 목록 조회
	 * @param expertUserId 전문가 User ID
	 * @param page 페이지 번호
	 * @param size 페이지 사이즈
	 * @return 전문가의 후기 목록
	 */
	List<ReviewSummaryResponseDTO> findReviewsByExpertUserId(Long expertUserId, int page, int size);
}
