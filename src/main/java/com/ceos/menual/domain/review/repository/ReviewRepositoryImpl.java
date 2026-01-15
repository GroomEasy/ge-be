package com.ceos.menual.domain.review.repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.ceos.menual.domain.review.dto.response.AvailableReviewResponseDTO;
import com.ceos.menual.domain.review.dto.response.CompletedReviewResponseDTO;
import com.ceos.menual.entity.*;
import com.ceos.menual.entity.enums.Category;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.stereotype.Repository;

import com.ceos.menual.domain.review.dto.response.ReviewSummaryResponseDTO;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepository {

	private final JPAQueryFactory queryFactory;

	private static final QReview r = QReview.review;
	private static final QConsultation c = QConsultation.consultation;
	private static final QExpertProfile ep = QExpertProfile.expertProfile;
	private static final QReviewImage ri = QReviewImage.reviewImage;
	private static final QUser u = QUser.user;


	@Override
	public Long countByConsultationGeneralProfileId(Long generalProfileId) {
		return queryFactory
				.select(r.count())
				.from(r)
				.join(r.consultation, c)
				.where(c.generalProfile.id.eq(generalProfileId))
				.fetchOne();
	}


	@Override
	public List<ReviewSummaryResponseDTO> findRecentReviews(Category category, int page, int size) {

		List<Tuple> results = queryFactory
				.select(
						r.id,
						r.rating,
						r.content,
						ep.category,
						r.createdAt,
						u.nickname,
						u.profileImage,
						ep.id
				)
				.from(r)
				.join(r.consultation, c)
				.join(c.expertProfile, ep)
				.join(ep.user, u)
				.where(categoryEq(category))
				.orderBy(r.createdAt.desc())
				.offset((long) page * size)
				.limit(size)
				.fetch();

		if (results.isEmpty()) {
			return Collections.emptyList();
		}

		List<Long> reviewIds = results.stream()
				.map(t -> t.get(r.id))
				.collect(Collectors.toList());

		List<Long> expertIds = results.stream()
				.map(t -> t.get(ep.id))
				.distinct()
				.collect(Collectors.toList());

		Map<Long, List<String>> imagesMap = getReviewImagesInBatch(reviewIds);

		Map<Long, Double> expertRatingsMap = getExpertRatingsInBatch(expertIds);

		return results.stream()
				.map(tuple -> {
					List<String> mediaUrls = imagesMap.getOrDefault(
							tuple.get(r.id),
							Collections.emptyList()
					);

					Category cat = tuple.get(ep.category);
					LocalDateTime createdAt = tuple.get(r.createdAt);
					Long expertId = tuple.get(ep.id);

					return ReviewSummaryResponseDTO.builder()
							.reviewId(tuple.get(r.id))
							.rating(tuple.get(r.rating))
							.content(tuple.get(r.content))
							.mediaUrls(mediaUrls)
							.category(cat != null ? cat.name() : null)
							.createdAt(createdAt != null ? createdAt.toString() : null)
							.expertNickname(tuple.get(u.nickname))
							.expertProfileImage(tuple.get(u.profileImage))
							.expertRatingAverage(expertRatingsMap.getOrDefault(expertId, 0.0))
							.build();
				})
				.collect(Collectors.toList());
	}

	@Override
	public List<ReviewSummaryResponseDTO> findBestReviews(Category category) {

		List<Tuple> results = queryFactory
				.select(
						r.id,
						r.rating,
						r.content,
						ep.category,
						r.createdAt
				)
				.from(r)
				.join(r.consultation, c)
				.join(c.expertProfile, ep)
				.where(categoryEq(category))
				.orderBy(r.likeCount.desc(), r.createdAt.desc())
				.limit(10)
				.fetch();

		if (results.isEmpty()) {
			return Collections.emptyList();
		}

		List<Long> reviewIds = results.stream()
				.map(t -> t.get(r.id))
				.collect(Collectors.toList());

		Map<Long, List<String>> imagesMap = getReviewImagesInBatch(reviewIds);

		return results.stream()
				.map(tuple -> {
					List<String> mediaUrls = imagesMap.getOrDefault(
							tuple.get(r.id),
							Collections.emptyList()
					);

					Category cat = tuple.get(ep.category);
					LocalDateTime createdAt = tuple.get(r.createdAt);

					return ReviewSummaryResponseDTO.builder()
							.reviewId(tuple.get(r.id))
							.rating(tuple.get(r.rating))
							.content(tuple.get(r.content))
							.mediaUrls(mediaUrls)
							.category(cat != null ? cat.name() : null)
							.createdAt(createdAt != null ? createdAt.toString() : null)
							.build();
				})
				.collect(Collectors.toList());
	}

	@Override
	public List<AvailableReviewResponseDTO> findAvailableReviews(Long generalProfileId) {
		return queryFactory
				.select(Projections.constructor(
						AvailableReviewResponseDTO.class,
						c.id,
						u.nickname,
						u.profileImage,
						ep.category,
						c.scheduleTime,
						ep.introduction
				))
				.from(c)
				.join(c.expertProfile, ep)
				.join(ep.user, u)
				.where(
						c.generalProfile.id.eq(generalProfileId),
						c.reviewWritten.isFalse(), // 후기 미작성
						c.status.in(
								com.ceos.menual.entity.enums.ConsultationStatus.IN_PROGRESS,
								com.ceos.menual.entity.enums.ConsultationStatus.COMPLETED
						)
				)
				.orderBy(c.scheduleTime.desc())
				.fetch();
	}

	@Override
	public List<CompletedReviewResponseDTO> findCompletedReviews(Long generalProfileId) {
		// Review + Consultation + ExpertProfile + User 조회
		List<Tuple> results = queryFactory
				.select(
						r.id,
						c.scheduleTime,
						u.nickname,
						r.rating,
						r.content
				)
				.from(r)
				.join(r.consultation, c)
				.join(c.expertProfile, ep)
				.join(ep.user, u)
				.where(
						c.generalProfile.id.eq(generalProfileId),
						c.reviewWritten.isTrue() // 후기 작성 완료
				)
				.orderBy(c.scheduleTime.desc())
				.fetch();

		if (results.isEmpty()) {
			return Collections.emptyList();
		}

		// reviewId 추출
		List<Long> reviewIds = results.stream()
				.map(t -> t.get(r.id))
				.collect(Collectors.toList());

		// 후기 이미지 배치 조회
		Map<Long, List<String>> imagesMap = getReviewImagesInBatch(reviewIds);

		// 후기 해시태그 배치 조회
		Map<Long, List<String>> hashtagsMap = getReviewHashtagsInBatch(reviewIds);

		// DTO 조립
		return results.stream()
				.map(tuple -> CompletedReviewResponseDTO.builder()
						.reviewId(tuple.get(r.id))
						.consultationDate(tuple.get(c.scheduleTime))
						.expertName(tuple.get(u.nickname))
						.rating(tuple.get(r.rating))
						.content(tuple.get(r.content))
						.imageUrls(imagesMap.getOrDefault(tuple.get(r.id), Collections.emptyList()))
						.hashtags(hashtagsMap.getOrDefault(tuple.get(r.id), Collections.emptyList()))
						.build())
				.collect(Collectors.toList());
	}

	// ======== 헬퍼 메서드 ======== //

	private Map<Long, List<String>> getReviewImagesInBatch(List<Long> reviewIds) {

		List<Tuple> images = queryFactory
				.select(
						ri.review.id,
						ri.imageUrl
				)
				.from(ri)
				.where(ri.review.id.in(reviewIds))
				.orderBy(
						ri.review.id.asc(),
						ri.displayOrder.asc()
				)
				.fetch();

		Map<Long, List<String>> result = new LinkedHashMap<>();

		for (Tuple tuple : images) {
			Long reviewId = tuple.get(ri.review.id);
			String imageUrl = tuple.get(ri.imageUrl);

			result.computeIfAbsent(reviewId, k -> new ArrayList<>());
			result.get(reviewId).add(imageUrl);
		}

		return result;
	}

	/**
	 * 여러 전문가의 평균 평점을 배치로 조회
	 */
	private Map<Long, Double> getExpertRatingsInBatch(List<Long> expertIds) {

		List<Tuple> ratings = queryFactory
				.select(
						ep.id,
						r.rating.avg()
				)
				.from(r)
				.join(r.consultation, c)
				.join(c.expertProfile, ep)
				.where(ep.id.in(expertIds))
				.groupBy(ep.id)
				.fetch();

		Map<Long, Double> result = new HashMap<>();

		for (Tuple tuple : ratings) {
			Long expertId = tuple.get(ep.id);
			Double avgRating = tuple.get(r.rating.avg());
			result.put(expertId, avgRating != null ? avgRating : 0.0);
		}

		return result;
	}

	/**
	 * 후기 해시태그 배치 조회
	 */
	private Map<Long, List<String>> getReviewHashtagsInBatch(List<Long> reviewIds) {
		if (reviewIds == null || reviewIds.isEmpty()) {
			return Collections.emptyMap();
		}

		QReviewHashtag rh = QReviewHashtag.reviewHashtag;
		QHashtag h = QHashtag.hashtag;

		List<Tuple> hashtagTuples = queryFactory
				.select(rh.review.id, h.name)
				.from(rh)
				.join(rh.hashtag, h)
				.where(rh.review.id.in(reviewIds))
				.fetch();

		return hashtagTuples.stream()
				.collect(Collectors.groupingBy(
						tuple -> tuple.get(rh.review.id),
						Collectors.mapping(
								tuple -> tuple.get(h.name),
								Collectors.toList()
						)
				));
	}

	private BooleanExpression categoryEq(Category category) {
		return category != null ? ep.category.eq(category) : null;
	}
}