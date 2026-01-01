package com.ceos.menual.domain.expert.repository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.ceos.menual.domain.expert.dto.response.ExpertSummaryResponseDTO;
import com.ceos.menual.entity.QReview;
import com.ceos.menual.entity.QUser;
import com.ceos.menual.entity.enums.Category;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import org.springframework.stereotype.Repository;

import com.ceos.menual.domain.expert.dto.response.ExpertRankingResponseDTO;
import com.ceos.menual.entity.QConsultation;
import com.ceos.menual.entity.QExpertProfile;
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
			.where(ep.category.eq(category))
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
	public List<ExpertSummaryResponseDTO> findExpertList(Category category, int page, int size) {

		List<ExpertSummaryResponseDTO> results = queryFactory
				.select(Projections.constructor(
						ExpertSummaryResponseDTO.class,
						ep.id,
						u.nickname,
						ep.category.stringValue(),
						u.profileImage,
						ep.introduction,

						// 평점 평균
						ExpressionUtils.as(
								JPAExpressions.select(r.rating.avg())
										.from(r)
										.join(r.consultation, c)
										.where(c.expertProfile.eq(ep)),
								"ratingAverage"
						),

						// 리뷰 개수
						ExpressionUtils.as(
								JPAExpressions.select(r.count())
										.from(r)
										.join(r.consultation, c)
										.where(c.expertProfile.eq(ep)),
								"reviewCount"
						),

						Expressions.nullExpression()
				))
				.from(ep)
				.join(ep.user, u)
				.where(categoryEq(category))
				.orderBy(ep.id.desc())
				.offset((long) page * size)
				.limit(size)
				.fetch();

		// 조회된 각 전문가마다 최신 리뷰 이미지 3개를 조회해서 채워 넣음
		results.forEach(dto -> {
			List<String> images = getTop3ReviewImages(dto.getExpertId());
			dto.setImages(images);
		});

		return results;
	}

	// 전문가 ID로 최신 리뷰 이미지 3개를 가져오는 메서드
	private List<String> getTop3ReviewImages(Long expertProfileId) {
		// 최근 리뷰들의 이미지 문자열들을 가져옴
		List<String> rawUrls = queryFactory
				.select(r.mediaUrls)
				.from(r)
				.join(r.consultation, c)
				.where(c.expertProfile.id.eq(expertProfileId)
						.and(r.mediaUrls.isNotNull()))
				.orderBy(r.createdAt.desc())
				.limit(3)
				.fetch();

		// 콤마로 쪼개고 리스트로 합치고 최대 3개까지만 자르기
		return rawUrls.stream()
				.flatMap(urls -> Arrays.stream(urls.split(",")))
				.limit(3)
				.collect(Collectors.toList());
	}

	private BooleanExpression categoryEq(Category category) {
		return category != null ? ep.category.eq(category) : null;
	}

}
