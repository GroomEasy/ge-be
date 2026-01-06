package com.ceos.menual.domain.expert.repository;

import java.util.*;
import java.util.stream.Collectors;

import com.ceos.menual.domain.expert.dto.response.ExpertSummaryResponseDTO;
import com.ceos.menual.entity.*;
import com.ceos.menual.entity.enums.Category;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.stereotype.Repository;

import com.ceos.menual.domain.expert.dto.response.ExpertRankingResponseDTO;
import com.ceos.menual.entity.enums.ConsultationStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ExpertRepositoryImpl implements ExpertRepository {
	private final JPAQueryFactory queryFactory;

	private static final QUser u = QUser.user;
	private static final QExpertProfile ep = QExpertProfile.expertProfile;
	private static final QConsultation c = QConsultation.consultation;
	private static final QReview r = QReview.review;
	private static final QReviewImage ri = QReviewImage.reviewImage;


	@Override
	public List<ExpertRankingResponseDTO> findTop3Overall() {
		return queryFactory
			.select(Projections.constructor(
				ExpertRankingResponseDTO.class,
				ep.user.nickname,
				ep.category,
				ep.user.profileImage,
				ep.introduction
			))
			.from(ep)
			.leftJoin(c)
				.on(c.expertProfile.id.eq(ep.id)
					.and(c.status.eq(ConsultationStatus.COMPLETED)))
			.join(ep.user)
			.groupBy(
				ep.id,
				ep.user.nickname,
				ep.category,
				ep.user.profileImage,
				ep.introduction
			)
			.orderBy(c.id.count().desc())
			.limit(3)
			.fetch();
	}

	@Override
	public List<ExpertRankingResponseDTO> findTop3ByCategory(Category category) {
		QExpertLike el = QExpertLike.expertLike;

		return queryFactory
				.select(Projections.constructor(
						ExpertRankingResponseDTO.class,
						ep.user.nickname,
						ep.category,
						ep.user.profileImage,
						ep.introduction
				))
				.from(ep)
				.leftJoin(el)
				.on(el.expertProfile.id.eq(ep.id))
				.join(ep.user)
				.where(ep.category.eq(category))
				.groupBy(
						ep.id,
						ep.user.nickname,
						ep.category,
						ep.user.profileImage,
						ep.introduction
				)
				.orderBy(el.id.count().desc())
				.limit(3)
				.fetch();
	}

	@Override
	public List<ExpertSummaryResponseDTO> findExpertList(Category category, int page, int size) {

		// 전문가 조회
		List<Tuple> results = queryFactory
				.select(
						ep.id,
						u.nickname,
						ep.category,
						u.profileImage,
						ep.introduction,
						r.rating.avg().coalesce(0.0),
						r.count()
				)
				.from(ep)
				.join(ep.user, u)
				.leftJoin(c).on(c.expertProfile.eq(ep))
				.leftJoin(r).on(r.consultation.eq(c))
				.where(categoryEq(category))
				.groupBy(ep.id, u.nickname, ep.category, u.profileImage, ep.introduction)
				.orderBy(
						r.count().desc(),           // 리뷰 많은 순
						r.rating.avg().desc()       // 평점 높은 순
				)
				.offset((long) page * size)
				.limit(size)
				.fetch();

		// 아무것도 없으면 그냥 반환
		if (results.isEmpty()) {
			return Collections.emptyList();
		}

		// 전문가 ID 추출
		List<Long> expertIds = results.stream()
				.map(t -> t.get(ep.id))
				.collect(Collectors.toList());

		// 전문가 ID로 이미지 조회
		Map<Long, List<String>> imagesMap = getReviewImagesInBatch(expertIds);

		// DTO 조립
		return results.stream()
				.map(tuple -> ExpertSummaryResponseDTO.builder()
						.expertId(tuple.get(ep.id))
						.nickname(tuple.get(u.nickname))
						.category(tuple.get(ep.category).name())
						.profileImage(tuple.get(u.profileImage))
						.introduction(tuple.get(ep.introduction))
						.ratingAverage(tuple.get(r.rating.avg().coalesce(0.0)))
						.reviewCount(tuple.get(r.count()))
						.representativeReviewImages(
								imagesMap.getOrDefault(tuple.get(ep.id), Collections.emptyList())
						)
						.build())
				.collect(Collectors.toList());
	}

	/**
	 * 여러 전문가의 리뷰 이미지를 배치로 조회
	 * 각 전문가당 최신 리뷰 이미지 3개까지
	 */
	private Map<Long, List<String>> getReviewImagesInBatch(List<Long> expertProfileIds) {

		List<Tuple> images = queryFactory
				.select(
						c.expertProfile.id,
						ri.imageUrl,
						r.createdAt
				)
				.from(ri)
				.join(ri.review, r)
				.join(r.consultation, c)
				.where(c.expertProfile.id.in(expertProfileIds))
				.orderBy(
						c.expertProfile.id.asc(),
						r.createdAt.desc(),
						ri.displayOrder.asc()
				)
				.fetch();

		// 전문가별로 그룹핑 (최대 3개)
		Map<Long, List<String>> result = new LinkedHashMap<>();

		for (Tuple tuple : images) {
			Long expertId = tuple.get(c.expertProfile.id);
			String imageUrl = tuple.get(ri.imageUrl);

			result.computeIfAbsent(expertId, k -> new ArrayList<>());

			List<String> expertImages = result.get(expertId);
			if (expertImages.size() < 3 && !expertImages.contains(imageUrl)) {
				expertImages.add(imageUrl);
			}
		}

		return result;
	}

	private BooleanExpression categoryEq(Category category) {
		return category != null ? ep.category.eq(category) : null;
	}
}
