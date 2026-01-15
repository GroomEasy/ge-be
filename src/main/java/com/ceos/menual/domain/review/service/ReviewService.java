package com.ceos.menual.domain.review.service;

import java.util.List;

import com.ceos.menual.domain.common.service.S3PresignedUrlService;
import com.ceos.menual.domain.consultation.repository.ConsultationRepository;
import com.ceos.menual.domain.reservation.exception.ReservationErrorCode;
import com.ceos.menual.domain.review.dto.request.CreateReviewRequestDTO;
import com.ceos.menual.domain.review.dto.response.AvailableReviewResponseDTO;
import com.ceos.menual.domain.review.dto.response.CompletedReviewResponseDTO;
import com.ceos.menual.domain.review.dto.response.CreateReviewResponseDTO;
import com.ceos.menual.domain.review.exception.ReviewErrorCode;
import com.ceos.menual.domain.review.repository.HashtagRepository;
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
	private final HashtagRepository hashtagRepository;
	private final S3PresignedUrlService s3PresignedUrlService;

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

		// 해시태그 검증
		validateHashtags(request.getHashtags());

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

		// 이미지가 있으면 tmp → final 이동 후 ReviewImage 생성
		if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
			processReviewImages(savedReview, request.getImageUrls());
		}

		// 해시태그 처리
		if (request.getHashtags() != null && !request.getHashtags().isEmpty()) {
			processHashtags(savedReview, request.getHashtags());
		}

		// Consultation의 reviewWritten을 true로 변경
		consultation.markReviewAsWritten();

		log.info("후기 작성 완료 - reviewId: {}", savedReview.getId());

		return CreateReviewResponseDTO.of(savedReview.getId(), consultation.getId());
	}

	// =========== 비즈니스 메서드 ============ //

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

	/**
	 * 해시태그 검증
	 */
	private void validateHashtags(List<String> hashtags) {
		if (hashtags == null || hashtags.isEmpty()) {
			return;
		}

		// 개수 검증
		if (hashtags.size() > 5) {
			throw new GlobalException(ReviewErrorCode.TOO_MANY_HASHTAGS);
		}

		// 각 해시태그 검증
		for (String hashtag : hashtags) {
			// 빈 문자열 체크
			if (hashtag == null || hashtag.trim().isEmpty()) {
				throw new GlobalException(ReviewErrorCode.EMPTY_HASHTAG);
			}

			String trimmedHashtag = hashtag.trim();

			// 실제 문자 개수로 길이 검증 (한글 기준 6글자)
			int actualLength = trimmedHashtag.codePointCount(0, trimmedHashtag.length());
			if (actualLength > 6) {
				throw new GlobalException(ReviewErrorCode.HASHTAG_TOO_LONG);
			}
		}
	}

	/**
	 * 해시태그 처리
	 * - 기존 해시태그가 있으면 재사용
	 * - 없으면 새로 생성
	 * - 사용 횟수 증가
	 */
	private void processHashtags(Review review, List<String> hashtagNames) {
		for (String hashtagName : hashtagNames) {
			String trimmedName = hashtagName.trim();

			// 기존 해시태그 찾기 or 새로 생성
			Hashtag hashtag = hashtagRepository.findByName(trimmedName)
					.orElseGet(() -> {
						Hashtag newHashtag = Hashtag.builder()
								.name(trimmedName)
								.usageCount(0)
								.build();
						return hashtagRepository.save(newHashtag);
					});

			// 사용 횟수 증가
			hashtag.incrementUsageCount();

			// ReviewHashtag 중간 테이블 생성
			ReviewHashtag reviewHashtag = ReviewHashtag.builder()
					.review(review)
					.hashtag(hashtag)
					.build();

			// Review에 추가
			review.addHashtag(reviewHashtag);
		}
	}

	/**
	 * 후기 이미지 처리: tmp → final 이동 및 ReviewImage 생성
	 */
	private void processReviewImages(Review savedReview, List<String> tempImageUrls) {
		Long reviewId = savedReview.getId();

		for (int i = 0; i < tempImageUrls.size(); i++) {
			String tempImageUrl = tempImageUrls.get(i);

			try {
				// 1. tmp 경로에서 정보 추출
				// 예: tmp/review/reservation-67/front/image.png
				String[] pathParts = tempImageUrl.split("/");

				if (pathParts.length < 5) {
					log.error("잘못된 이미지 경로 형식 - reviewId: {}, path: {}", reviewId, tempImageUrl);
					throw new GlobalException(ReviewErrorCode.INVALID_IMAGE_PATH);
				}

				String imageType = pathParts[3];  // "front"
				String fileName = pathParts[4];   // "image.png"

				// 최종 S3 경로 생성
				// final/review/{reviewId}/{imageType}/{fileName}
				String finalS3Key = String.format("final/review/%d/%s/%s",
						reviewId, imageType, fileName);

				// S3에서 이미지 이동 (tmp → final)
				s3PresignedUrlService.moveImageFromTempToFinal(tempImageUrl, finalS3Key);
				log.info("리뷰 이미지 이동 완료 - reviewId: {}, from: {}, to: {}",
						reviewId, tempImageUrl, finalS3Key);

				// 공개 S3 URL 생성
				String finalImageUrl = s3PresignedUrlService.generateS3Url(finalS3Key);

				// ReviewImage 엔티티 생성 (S3 key만 저장)
				ReviewImage reviewImage = ReviewImage.builder()
						.imageUrl(finalS3Key)
						.displayOrder(i)
						.build();

				// Review와 양방향 관계 설정
				reviewImage.setReview(savedReview);

				log.info("ReviewImage 생성 완료 - reviewId: {}, order: {}, url: {}",
						reviewId, i, finalImageUrl);

			} catch (GlobalException e) {
				throw e;
			} catch (Exception e) {
				log.error("리뷰 이미지 처리 실패 - reviewId: {}, tempUrl: {}",
						reviewId, tempImageUrl, e);
				throw new GlobalException(ReviewErrorCode.IMAGE_PROCESSING_FAILED);
			}
		}
	}


}
