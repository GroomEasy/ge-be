package com.ceos.menual.domain.expert.repository;

import java.util.List;

import com.ceos.menual.entity.enums.Category;
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

	private static final QExpertProfile ep = QExpertProfile.expertProfile;
	private static final QConsultation c = QConsultation.consultation;

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
}
