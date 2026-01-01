package com.ceos.menual.domain.review.repository;

import java.util.*;
import java.util.stream.Collectors;

import com.ceos.menual.entity.QConsultation;
import com.ceos.menual.entity.QExpertProfile;
import com.ceos.menual.entity.QReviewImage;
import com.ceos.menual.entity.enums.Category;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.stereotype.Repository;

import com.ceos.menual.domain.review.dto.response.ReviewSummaryResponseDTO;
import com.ceos.menual.entity.QReview;
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

	@Override
	public List<ReviewSummaryResponseDTO> findRecentReviews(Category category, int page, int size) {

		// 리뷰 기본 정보 조회
		List<Tuple> results = queryFactory
				.select(
						r.id,
						r.rating,
						r.content,
						r.likeCount,
						ep.category,
						r.createdAt
				)
				.from(r)
				.join(r.consultation, c)
				.join(c.expertProfile, ep)
				.where(categoryEq(category))
				.orderBy(r.createdAt.desc())
				.offset((long) page * size)
				.limit(size)
				.fetch();

		if (results.isEmpty()) {
			return Collections.emptyList();
		}

		// 리뷰 ID 추출
		List<Long> reviewIds = results.stream()
				.map(t -> t.get(r.id))
				.collect(Collectors.toList());

		// 이미지 배치 조회
		Map<Long, List<String>> imagesMap = getReviewImagesInBatch(reviewIds);

		// DTO 조립
		return results.stream()
				.map(tuple -> {
					List<String> images = imagesMap.getOrDefault(tuple.get(r.id), Collections.emptyList());
					// List를 쉼표로 구분된 String으로 변환 (DTO가 String 타입이므로)
					String mediaUrls = images.isEmpty() ? null : String.join(",", images);

					return ReviewSummaryResponseDTO.builder()
							.reviewId(tuple.get(r.id))
							.rating(tuple.get(r.rating))
							.content(tuple.get(r.content))
							.mediaUrls(mediaUrls)
							.likeCount(tuple.get(r.likeCount))
							.category(tuple.get(ep.category).name())
							.createdAt(tuple.get(r.createdAt).toString())
							.build();
				})
				.collect(Collectors.toList());
	}

	@Override
	public List<ReviewSummaryResponseDTO> findBestReviews(Category category) {

		// 리뷰 기본 정보 조회 (베스트순)
		List<Tuple> results = queryFactory
				.select(
						r.id,
						r.rating,
						r.content,
						r.likeCount,
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

		// 리뷰 ID 추출
		List<Long> reviewIds = results.stream()
				.map(t -> t.get(r.id))
				.collect(Collectors.toList());

		// 이미지 배치 조회
		Map<Long, List<String>> imagesMap = getReviewImagesInBatch(reviewIds);

		// DTO 조립
		return results.stream()
				.map(tuple -> {
					List<String> images = imagesMap.getOrDefault(tuple.get(r.id), Collections.emptyList());
					String mediaUrls = images.isEmpty() ? null : String.join(",", images);

					return ReviewSummaryResponseDTO.builder()
							.reviewId(tuple.get(r.id))
							.rating(tuple.get(r.rating))
							.content(tuple.get(r.content))
							.mediaUrls(mediaUrls)
							.likeCount(tuple.get(r.likeCount))
							.category(tuple.get(ep.category).name())
							.createdAt(tuple.get(r.createdAt).toString())
							.build();
				})
				.collect(Collectors.toList());
	}

	/**
	 * 여러 리뷰의 이미지를 배치로 조회
	 */
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

		// 리뷰별로 그룹핑
		Map<Long, List<String>> result = new LinkedHashMap<>();

		for (Tuple tuple : images) {
			Long reviewId = tuple.get(ri.review.id);
			String imageUrl = tuple.get(ri.imageUrl);

			result.computeIfAbsent(reviewId, k -> new ArrayList<>());
			result.get(reviewId).add(imageUrl);
		}

		return result;
	}

	private BooleanExpression categoryEq(Category category) {
		return category != null ? ep.category.eq(category) : null;
	}
}