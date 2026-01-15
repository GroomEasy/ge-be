package com.ceos.menual.domain.expert.repository;

import com.ceos.menual.domain.expert.dto.response.ExpertSummaryResponseDTO;
import com.ceos.menual.entity.*;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import com.ceos.menual.entity.enums.Category;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ExpertLikeRepositoryImpl implements ExpertLikeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // Q클래스들
    private static final QExpertLike el = QExpertLike.expertLike;
    private static final QExpertProfile ep = QExpertProfile.expertProfile;
    private static final QUser u = QUser.user;
    private static final QConsultation c = QConsultation.consultation;
    private static final QReview r = QReview.review;
    private static final QReviewImage ri = QReviewImage.reviewImage;

    @Override
    public List<ExpertSummaryResponseDTO> findLikedExpertList(Long generalProfileId, Category category, int page, int size) {

        List<Tuple> results = queryFactory
                .select(
                        u.id,
                        u.nickname,
                        ep.category,
                        u.profileImage,
                        ep.introduction,
                        r.rating.avg().coalesce(0.0),
                        r.count()
                )
                .from(el)
                .join(el.expertProfile, ep)
                .join(ep.user, u)
                .leftJoin(c).on(c.expertProfile.eq(ep))
                .leftJoin(r).on(r.consultation.eq(c))
                .where(
                        el.generalProfile.id.eq(generalProfileId), // 내 찜 목록만
                        categoryEq(category)
                )
                .groupBy(u.id, u.nickname, ep.category, u.profileImage, ep.introduction, el.createdAt)
                .orderBy(el.createdAt.desc()) // 찜한 최신순
                .offset((long) page * size)
                .limit(size)
                .fetch();

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = results.stream()
                .map(t -> t.get(u.id))
                .collect(Collectors.toList());

        Map<Long, List<String>> imagesMap = getReviewImagesInBatch(userIds);

        return results.stream()
                .map(tuple -> ExpertSummaryResponseDTO.builder()
                        .expertId(tuple.get(u.id))
                        .nickname(tuple.get(u.nickname))
                        .category(tuple.get(ep.category).name())
                        .profileImage(tuple.get(u.profileImage))
                        .introduction(tuple.get(ep.introduction))
                        .ratingAverage(tuple.get(r.rating.avg().coalesce(0.0)))
                        .reviewCount(tuple.get(r.count()))
                        .representativeReviewImages(
                                imagesMap.getOrDefault(tuple.get(u.id), Collections.emptyList())
                        )
                        .build())
                .collect(Collectors.toList());
    }

    private BooleanExpression categoryEq(Category category) {
        return category != null ? ep.category.eq(category) : null;
    }

    private Map<Long, List<String>> getReviewImagesInBatch(List<Long> userIds) {
        List<Tuple> images = queryFactory
                .select(ep.user.id, ri.imageUrl)
                .from(ri)
                .join(ri.review, r)
                .join(r.consultation, c)
                .join(c.expertProfile, ep)
                .where(ep.user.id.in(userIds))
                .orderBy(ep.user.id.asc(), r.createdAt.desc(), ri.displayOrder.asc())
                .fetch();

        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (Tuple tuple : images) {
            Long userId = tuple.get(ep.user.id);
            String imageUrl = tuple.get(ri.imageUrl);
            result.computeIfAbsent(userId, k -> new ArrayList<>());
            List<String> list = result.get(userId);
            if (list.size() < 3 && !list.contains(imageUrl)) {
                list.add(imageUrl);
            }
        }
        return result;
    }
}