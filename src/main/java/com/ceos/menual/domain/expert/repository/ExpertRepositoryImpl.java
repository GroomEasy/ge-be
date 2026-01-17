package com.ceos.menual.domain.expert.repository;

import java.util.*;
import java.util.stream.Collectors;

import com.ceos.menual.domain.expert.dto.response.ExpertPortfolioResponseDTO;
import com.ceos.menual.domain.expert.dto.response.ExpertSummaryResponseDTO;
import com.ceos.menual.domain.expert.exception.ExpertErrorCode;
import com.ceos.menual.entity.*;
import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.global.exception.GlobalException;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import com.ceos.menual.domain.expert.dto.response.ExpertRankingResponseDTO;
import com.ceos.menual.entity.enums.ConsultationStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import static com.ceos.menual.entity.QExpertProfile.expertProfile;
import static com.ceos.menual.entity.QHashtag.hashtag;
import static com.ceos.menual.entity.QPortfolio.portfolio;
import static com.ceos.menual.entity.QPortfolioHashtag.portfolioHashtag;

@Repository
@RequiredArgsConstructor
public class ExpertRepositoryImpl implements ExpertRepository {
	private final JPAQueryFactory queryFactory;
	private final EntityManager entityManager;

	private static final QUser u = QUser.user;
	private static final QExpertProfile ep = expertProfile;
	private static final QConsultation c = QConsultation.consultation;
	private static final QReview r = QReview.review;
	private static final QReviewImage ri = QReviewImage.reviewImage;
	private static final QExpertLike el = QExpertLike.expertLike;


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
	public List<ExpertRankingResponseDTO> findTop3ByCategory(Category category, Long currentUserId) {
		return queryFactory
				.select(Projections.constructor(
						ExpertRankingResponseDTO.class,
						ep.user.nickname,
						ep.category,
						ep.user.profileImage,
						ep.introduction,
						isLikedExpression(currentUserId)
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
				.from(ep)
				.join(ep.user, u)
				.leftJoin(c).on(c.expertProfile.eq(ep))
				.leftJoin(r).on(r.consultation.eq(c))
				.where(categoryEq(category))
				.groupBy(u.id, u.nickname, ep.category, u.profileImage, ep.introduction)
				.orderBy(
						r.count().desc(),
						r.rating.avg().desc()
				)
				.offset((long) page * size)
				.limit(size)
				.fetch();

		if (results.isEmpty()) {
			return Collections.emptyList();
		}

		// userId 추출
		List<Long> userIds = results.stream()
				.map(t -> t.get(u.id))
				.collect(Collectors.toList());

		// 리뷰 이미지 조회 (userId 기준)
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

	@Override
	public List<ExpertPortfolioResponseDTO> findPortfolioList(Long expertUserId, int page, int size) {
		// 포트폴리오 기본 정보 조회
		List<Tuple> results = queryFactory
				.select(
						portfolio.id,
						portfolio.title,
						portfolio.concern,
						portfolio.solution,
						portfolio.beforeImage,
						portfolio.afterImage,
						portfolio.isRepresentative
				)
				.from(portfolio)
				.join(portfolio.expertProfile, expertProfile)
				.where(expertProfile.user.id.eq(expertUserId))
				.orderBy(
						portfolio.isRepresentative.desc(),
						portfolio.createdAt.desc()
				)
				.offset((long) page * size)
				.limit(size)
				.fetch();

		if (results.isEmpty()) {
			return Collections.emptyList();
		}

		// 포트폴리오 ID 추출
		List<Long> portfolioIds = results.stream()
				.map(t -> t.get(portfolio.id))
				.collect(Collectors.toList());

		// 해시태그 배치 조회
		Map<Long, List<String>> hashtagMap = getPortfolioHashtagsInBatch(portfolioIds);

		// DTO 조립
		return results.stream()
				.map(tuple -> ExpertPortfolioResponseDTO.builder()
						.id(tuple.get(portfolio.id))
						.title(tuple.get(portfolio.title))
						.concern(tuple.get(portfolio.concern))
						.solution(tuple.get(portfolio.solution))
						.beforeImage(tuple.get(portfolio.beforeImage))
						.isRepresentative(tuple.get(portfolio.isRepresentative))
						.afterImage(tuple.get(portfolio.afterImage))
						.hashtags(hashtagMap.getOrDefault(tuple.get(portfolio.id), Collections.emptyList()))
						.build())
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public ExpertPortfolioResponseDTO savePortfolio(Portfolio portfolio, List<String> hashtagNames) {
		// 포트폴리오 저장
		entityManager.persist(portfolio);
		entityManager.flush();

		// 해시태그 처리
		List<String> savedHashtags = new ArrayList<>();
		if (hashtagNames != null && !hashtagNames.isEmpty()) {
			for (String hashtagName : hashtagNames) {
				// 해시태그 조회 또는 생성
				Hashtag hashtag = queryFactory
						.selectFrom(QHashtag.hashtag)
						.where(QHashtag.hashtag.name.eq(hashtagName))
						.fetchOne();

				if (hashtag == null) {
					// 새 해시태그 생성
					hashtag = Hashtag.builder()
							.name(hashtagName)
							.build();
					entityManager.persist(hashtag);
					entityManager.flush();
				}

				// PortfolioHashtag 연결
				PortfolioHashtag portfolioHashtag = PortfolioHashtag.builder()
						.portfolio(portfolio)
						.hashtag(hashtag)
						.build();

				entityManager.persist(portfolioHashtag);
				savedHashtags.add(hashtagName);
			}
		}

		// DTO 반환
		return ExpertPortfolioResponseDTO.builder()
				.id(portfolio.getId())
				.title(portfolio.getTitle())
				.concern(portfolio.getConcern())
				.solution(portfolio.getSolution())
				.beforeImage(portfolio.getBeforeImage())
				.afterImage(portfolio.getAfterImage())
				.isRepresentative(portfolio.isRepresentative())
				.hashtags(savedHashtags)
				.build();
	}


	/**
	 * 포트폴리오 ID와 전문가 ID로 단일 조회 (소유권 확인용)
	 */
	@Override
	public Optional<Portfolio> findPortfolioByExpertUserIdAndPortfolioId(Long expertUserId, Long portfolioId) {
		Portfolio result = queryFactory
				.selectFrom(portfolio)
				.join(portfolio.expertProfile, ep)
				.where(
						portfolio.id.eq(portfolioId),
						ep.user.id.eq(expertUserId)
				)
				.fetchOne();

		return Optional.ofNullable(result);
	}

	/**
	 * 해당 전문가의 모든 포트폴리오의 대표 설정을 해제 (Bulk Update)
	 * 예외를 던지지 않음 (0개여도 OK)
	 */
	@Override
	@Transactional
	public void resetRepresentativePortfolio(Long expertUserId) {
		queryFactory
				.update(portfolio)
				.set(portfolio.isRepresentative, false)
				.where(portfolio.expertProfile.user.id.eq(expertUserId))
				.execute();

		entityManager.flush();
		entityManager.clear();
	}


	// ============== 비즈니스 메서드 ================ //

	/**
	 * 해시태그 배치 조회 메서드
	 */
	private Map<Long, List<String>> getPortfolioHashtagsInBatch(List<Long> portfolioIds) {
		List<Tuple> hashtagResults = queryFactory
				.select(portfolioHashtag.portfolio.id, hashtag.name)
				.from(portfolioHashtag)
				.join(portfolioHashtag.hashtag, hashtag)
				.where(portfolioHashtag.portfolio.id.in(portfolioIds))
				.fetch();

		return hashtagResults.stream()
				.collect(Collectors.groupingBy(
						t -> t.get(portfolioHashtag.portfolio.id),
						Collectors.mapping(t -> t.get(hashtag.name), Collectors.toList())
				));
	}

	/**
	 * 여러 전문가의 리뷰 이미지를 배치로 조회
	 * 각 전문가당 최신 리뷰 이미지 3개까지
	 */
	private Map<Long, List<String>> getReviewImagesInBatch(List<Long> userIds) {

		List<Tuple> images = queryFactory
				.select(
						ep.user.id,
						ri.imageUrl,
						r.createdAt
				)
				.from(ri)
				.join(ri.review, r)
				.join(r.consultation, c)
				.join(c.expertProfile, ep)
				.where(ep.user.id.in(userIds))
				.orderBy(
						ep.user.id.asc(),
						r.createdAt.desc(),
						ri.displayOrder.asc()
				)
				.fetch();

		// userId별로 그룹핑 (최대 3개)
		Map<Long, List<String>> result = new LinkedHashMap<>();

		for (Tuple tuple : images) {
			Long userId = tuple.get(ep.user.id);
			String imageUrl = tuple.get(ri.imageUrl);

			result.computeIfAbsent(userId, k -> new ArrayList<>());

			List<String> expertImages = result.get(userId);
			if (expertImages.size() < 3 && !expertImages.contains(imageUrl)) {
				expertImages.add(imageUrl);
			}
		}

		return result;
	}

	// 찜 여부를 판단하는 QueryDSL 표현식
	private BooleanExpression isLikedExpression(Long currentUserId) {
		if (currentUserId == null) {
			return Expressions.asBoolean(false);
		}

		return JPAExpressions
				.selectOne()
				.from(el)
				.where(el.expertProfile.eq(ep)
						.and(el.generalProfile.user.id.eq(currentUserId)))
				.exists();
	}

	private BooleanExpression categoryEq(Category category) {
		return category != null ? ep.category.eq(category) : null;
	}
}
