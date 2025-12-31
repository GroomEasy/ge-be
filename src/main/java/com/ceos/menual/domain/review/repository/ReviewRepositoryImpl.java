package com.ceos.menual.domain.review.repository;

import java.util.List;

import com.ceos.menual.entity.QConsultation;
import com.ceos.menual.entity.QExpertProfile;
import com.ceos.menual.entity.enums.Category;
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

	// Q클래스들 정의
	private static final QReview r = QReview.review;
	private static final QConsultation c = QConsultation.consultation;
	private static final QExpertProfile ep = QExpertProfile.expertProfile;

	@Override
	public List<ReviewSummaryResponseDTO> findRecentReviews(Category category, int page, int size) {
		return queryFactory
				.select(Projections.constructor(
						ReviewSummaryResponseDTO.class,
						r.id,
						r.rating,
						r.content,
						r.mediaUrls,
						r.likeCount,
						ep.category.stringValue(),
						r.createdAt.stringValue()
				))
				.from(r)
				.join(r.consultation, c)         // Review -> Consultation 조인
				.join(c.expertProfile, ep)       // Consultation -> ExpertProfile 조인
				.where(categoryEq(category))
				.orderBy(r.createdAt.desc())
				.offset((long) page * size)  // 추가
				.limit(size)                  // 수정
				.limit(10)
				.fetch();
	}

	@Override
	public List<ReviewSummaryResponseDTO> findBestReviews(Category category) {
		return queryFactory
				.select(Projections.constructor(
						ReviewSummaryResponseDTO.class,
						r.id,
						r.rating,
						r.content,
						r.mediaUrls,
						r.likeCount,
						ep.category.stringValue(),
						r.createdAt.stringValue()
				))
				.from(r)
				.join(r.consultation, c)         // Review -> Consultation 조인
				.join(c.expertProfile, ep)       // Consultation -> ExpertProfile 조인
				.where(categoryEq(category))
				.orderBy(r.likeCount.desc(), r.createdAt.desc())
				.limit(10)
				.fetch();
	}

	//
	// 카테고리 동적 쿼리 (null이면 전체 조회, 값이 있으면 필터링)
	private BooleanExpression categoryEq(Category category) {
		return category != null ? ep.category.eq(category) : null;
	}
}