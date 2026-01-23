package com.ceos.menual.domain.consultation.repository;

import com.ceos.menual.domain.consultation.dto.response.ConsultationHistoryResponseDTO;
import com.ceos.menual.domain.consultation.dto.response.SolutionListResponseDTO;
import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.entity.enums.ConsultationStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import static com.ceos.menual.entity.QConsultation.consultation;
import static com.ceos.menual.entity.QExpertLike.expertLike;
import static com.ceos.menual.entity.QExpertProfile.expertProfile;
import static com.ceos.menual.entity.QReservation.reservation;
import static com.ceos.menual.entity.QUser.user;

@RequiredArgsConstructor
public class ConsultationRepositoryImpl implements ConsultationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ConsultationHistoryResponseDTO> findConsultationHistory(
            Long generalProfileId,
            Category category
    ) {
        return queryFactory
                .select(Projections.constructor(
                        ConsultationHistoryResponseDTO.class,
                        consultation.id,
                        reservation.id,
                        user.id,
                        user.nickname,
                        user.profileImage,
                        reservation.category,
                        consultation.type,
                        consultation.scheduleTime,
                        reservation.price,
                        expertLike.count(), // 전문가 프로필에 대한 찜 개수
                        // 후기 작성 가능 여부: reviewWritten이 false이면 true, 아니면 false
                        new CaseBuilder()
                                .when(consultation.reviewWritten.isFalse()).then(true)
                                .otherwise(false),
                        consultation.reviewWritten
                ))
                .from(consultation)
                .join(consultation.expertProfile, expertProfile)
                .join(expertProfile.user, user)
                .join(reservation).on(reservation.consultation.eq(consultation))
                .leftJoin(expertLike).on(expertLike.expertProfile.eq(expertProfile))
                .where(
                        consultation.generalProfile.id.eq(generalProfileId),
                        categoryEq(category),
                        isCompletedOrInProgress()
                )
                .groupBy(
                        consultation.id,
                        reservation.id,
                        user.id,
                        user.nickname,
                        user.profileImage,
                        reservation.category,
                        consultation.type,
                        consultation.scheduleTime,
                        reservation.price,
                        consultation.reviewWritten,
                        expertProfile.id
                )
                .orderBy(consultation.scheduleTime.desc())
                .fetch();
    }

    /**
     * 카테고리 필터 조건
     */
    private BooleanExpression categoryEq(Category category) {
        return category != null ? reservation.category.eq(category) : null;
    }

    /**
     * 상담 상태 필터 (IN_PROGRESS 또는 COMPLETED)
     * - IN_PROGRESS: 진행 중인 상담
     * - COMPLETED: 완료된 상담
     */
    private BooleanExpression isCompletedOrInProgress() {
        return consultation.status.in(ConsultationStatus.IN_PROGRESS, ConsultationStatus.COMPLETED);
    }

    @Override
    public List<SolutionListResponseDTO> findSolutionList(
            Long generalProfileId,
            Category category
    ) {
        return queryFactory
                .select(Projections.constructor(
                        SolutionListResponseDTO.class,
                        consultation.id,
                        consultation.createdAt,
                        user.id,
                        user.nickname,
                        reservation.category,
                        consultation.type
                ))
                .from(consultation)
                .join(consultation.expertProfile, expertProfile)
                .join(expertProfile.user, user)
                .join(reservation).on(reservation.consultation.eq(consultation))
                .where(
                        consultation.generalProfile.id.eq(generalProfileId),
                        categoryEq(category),
                        isCompletedOrInProgress(),
                        hasSolution()
                )
                .orderBy(consultation.createdAt.desc())
                .fetch();
    }

    /**
     * 솔루션이 작성된 상담 필터
     */
    private BooleanExpression hasSolution() {
        return consultation.solution.isNotNull()
                .and(consultation.solution.isNotEmpty());
    }
}
