package com.ceos.menual.domain.review.service;

import java.util.List;

import com.ceos.menual.domain.consultation.repository.ConsultationRepository;
import com.ceos.menual.domain.reservation.exception.ReservationErrorCode;
import com.ceos.menual.domain.review.dto.request.CreateReviewRequestDTO;
import com.ceos.menual.domain.review.dto.response.AvailableReviewResponseDTO;
import com.ceos.menual.domain.review.dto.response.CompletedReviewResponseDTO;
import com.ceos.menual.domain.review.dto.response.CreateReviewResponseDTO;
import com.ceos.menual.domain.review.exception.ReviewErrorCode;
import com.ceos.menual.domain.review.repository.ReviewJpaRepository;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.*;
import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.entity.enums.ConsultationStatus;
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
	private final ConsultationRepository consultationRepository;
	private final ReviewJpaRepository reviewJpaRepository;

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

	/**
	 * 후기 작성
	 */
	@Transactional
	public CreateReviewResponseDTO createReview(Long userId, CreateReviewRequestDTO request) {
		log.info("후기 작성 시작 - userId: {}, consultationId: {}", userId, request.getConsultationId());

		// 사용자 조회
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

		// GeneralProfile 확인
		GeneralProfile generalProfile = user.getGeneralProfile();
		if (generalProfile == null) {
			throw new GlobalException(ReviewErrorCode.UNAUTHORIZED_REVIEW_ACCESS);
		}

		// Consultation 조회 (fetch join으로 ExpertProfile도 함께)
		Consultation consultation = consultationRepository.findByIdWithProfiles(request.getConsultationId())
				.orElseThrow(() -> new GlobalException(ReviewErrorCode.CONSULTATION_NOT_FOUND));

		// 권한 검증 (본인의 상담인지)
		if (!consultation.getGeneralProfile().getId().equals(generalProfile.getId())) {
			throw new GlobalException(ReviewErrorCode.UNAUTHORIZED_REVIEW_ACCESS);
		}

		// 후기 작성 가능 여부 검증
		validateReviewCreation(consultation);

		// Review 엔티티 생성
		Review review = Review.builder()
				.consultation(consultation)
				.rating(request.getRating())
				.content(request.getContent())
				.category(consultation.getExpertProfile().getCategory())
				.likeCount(0)
				.build();

		// Review 저장
		Review savedReview = reviewJpaRepository.save(review);

		// 이미지가 있으면 ReviewImage 생성 및 추가
		if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
			for (int i = 0; i < request.getImageUrls().size(); i++) {
				ReviewImage reviewImage = ReviewImage.builder()
						.imageUrl(request.getImageUrls().get(i))
						.displayOrder(i)
						.build();

				// Review와 양방향 관계 설정
				reviewImage.setReview(savedReview);
			}
		}

		// Consultation의 reviewWritten을 true로 변경
		consultation.markReviewAsWritten();

		log.info("후기 작성 완료 - reviewId: {}", savedReview.getId());

		return CreateReviewResponseDTO.of(savedReview.getId(), consultation.getId());
	}

	/**
	 * 후기 작성 가능 여부 검증
	 */
	private void validateReviewCreation(Consultation consultation) {
		// 이미 후기가 작성되었는지 확인
		if (Boolean.TRUE.equals(consultation.getReviewWritten())) {
			throw new GlobalException(ReviewErrorCode.REVIEW_ALREADY_WRITTEN);
		}

		// 상담이 완료되었는지 확인 (IN_PROGRESS 또는 COMPLETED만 가능)
		ConsultationStatus status = consultation.getStatus();
		if (status != ConsultationStatus.IN_PROGRESS && status != ConsultationStatus.COMPLETED) {
			throw new GlobalException(ReviewErrorCode.CONSULTATION_NOT_COMPLETED);
		}
	}

}
