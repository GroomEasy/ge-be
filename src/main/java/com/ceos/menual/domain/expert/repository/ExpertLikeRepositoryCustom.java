package com.ceos.menual.domain.expert.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ExpertLikeRepositoryImpl implements ExpertLikeRepository {
    @Override
    public List<ExpertSummaryResponseDTO> findLikedExpertList(Long generalProfileId, Category category, int page, int size) {

        QExpertLike el = QExpertLike.expertLike; // 찜 테이블 Q클래스 추가

        List<Tuple> results = queryFactory
                .select(
                        u.id,
                        u.nickname,
                        ep.category,
                        u.profileImage,
                        ep.introduction,
                        r.rating.avg().coalesce(0.0), // 평점 평균
                        r.count()                     // 리뷰 개수
                )
                .from(el) // 1. 찜 테이블에서 시작
                .join(el.expertProfile, ep) // 2. 전문가 프로필 조인
                .join(ep.user, u)           // 3. 전문가 유저 정보 조인
                // 4. 통계 산출을 위한 리뷰 조인 (기존 로직 유지)
                .leftJoin(c).on(c.expertProfile.eq(ep))
                .leftJoin(r).on(r.consultation.eq(c))
                .where(
                        el.generalProfile.id.eq(generalProfileId), // 내 찜 목록만
                        categoryEq(category)                       // 카테고리 필터 (기존 메서드 재사용)
                )
                .groupBy(u.id, u.nickname, ep.category, u.profileImage, ep.introduction, el.createdAt)
                .orderBy(el.createdAt.desc()) // 최신 찜한 순서대로 정렬
                .offset((long) page * size)
                .limit(size)
                .fetch();

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        // --- 아래는 기존 findExpertList와 동일한 DTO 변환 로직입니다 ---

        // userId 추출
        List<Long> userIds = results.stream()
                .map(t -> t.get(u.id))
                .collect(Collectors.toList());

        // 리뷰 이미지 조회 (기존 메서드 재사용)
        Map<Long, List<String>> imagesMap = getReviewImagesInBatch(userIds);

        // DTO 조립
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
}
