package com.ceos.menual.domain.review.service;

import java.util.List;

import com.ceos.menual.domain.reservation.exception.ReservationErrorCode;
import com.ceos.menual.domain.review.dto.response.AvailableReviewResponseDTO;
import com.ceos.menual.domain.review.dto.response.CompletedReviewResponseDTO;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.GeneralProfile;
import com.ceos.menual.entity.User;
import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.global.exception.GlobalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ceos.menual.domain.review.dto.response.ReviewSummaryResponseDTO;
import com.ceos.menual.domain.review.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

	private final ReviewRepository reviewRepository;
	private final UserRepository userRepository;

	public List<ReviewSummaryResponseDTO> getRecentReviews(Category category, int page, int size) {
		return reviewRepository.findRecentReviews(category, page, size);
	}

	public List<ReviewSummaryResponseDTO> getBestReviews(Category category) {
		return reviewRepository.findBestReviews(category);
	}

	/**
	 * 작성 가능한 후기 목록 조회
	 */
	public List<AvailableReviewResponseDTO> getAvailableReviews(Long userId) {
		log.info("작성 가능한 후기 조회 시작 - userId: {}", userId);

		// 사용자 조회
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

		// GeneralProfile 조회 (일반 회원만 후기 작성 가능)
		if (user.getGeneralProfile() == null) {
			throw new GlobalException(ReservationErrorCode.GENERAL_PROFILE_NOT_FOUND);
		}

		GeneralProfile generalProfile = user.getGeneralProfile();

		// 작성 가능한 후기 조회 (QueryDSL)
		List<AvailableReviewResponseDTO> availableReviews =
				reviewRepository.findAvailableReviews(generalProfile.getId());

		log.info("작성 가능한 후기 조회 완료 - 개수: {}", availableReviews.size());

		return availableReviews;
	}

	/**
	 * 작성 완료된 후기 목록 조회
	 */
	public List<CompletedReviewResponseDTO> getCompletedReviews(Long userId) {
		log.info("작성 완료된 후기 조회 시작 - userId: {}", userId);

		// 사용자 조회
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

		// GeneralProfile 조회 (일반 회원만 후기 작성 가능)
		if (user.getGeneralProfile() == null) {
			throw new GlobalException(ReservationErrorCode.GENERAL_PROFILE_NOT_FOUND);
		}

		GeneralProfile generalProfile = user.getGeneralProfile();

		// 작성 완료된 후기 조회 (QueryDSL)
		List<CompletedReviewResponseDTO> completedReviews =
				reviewRepository.findCompletedReviews(generalProfile.getId());

		log.info("작성 완료된 후기 조회 완료 - 개수: {}", completedReviews.size());

		return completedReviews;
	}

}
