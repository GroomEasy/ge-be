package com.ceos.menual.domain.review.repository;

import java.util.List;

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

	@Override
	public List<ReviewSummaryResponseDTO> findRecentReviews(Long categoryId) {
		return queryFactory
			.select(Projections.constructor(
				ReviewSummaryResponseDTO.class,
				r.id,
				r.rating,
				r.content,
				r.mediaUrls,
				r.likeCount,
				r.categoryId,
				r.createdAt.stringValue()
			))
			.from(r)
			.where(categoryId != null ? r.categoryId.eq(categoryId) : null)
			.orderBy(r.createdAt.desc())
			.limit(10)
			.fetch();
	}

	@Override
	public List<ReviewSummaryResponseDTO> findBestReviews(Long categoryId) {
		return queryFactory
			.select(Projections.constructor(
				ReviewSummaryResponseDTO.class,
				r.id,
				r.rating,
				r.content,
				r.mediaUrls,
				r.likeCount,
				r.categoryId,
				r.createdAt.stringValue()
			))
			.from(r)
			.where(categoryId != null ? r.categoryId.eq(categoryId) : null)
			.orderBy(r.likeCount.desc(), r.createdAt.desc())
			.limit(10)
			.fetch();
	}
}
