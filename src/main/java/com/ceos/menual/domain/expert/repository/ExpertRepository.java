package com.ceos.menual.domain.expert.repository;

import java.util.List;
import java.util.Optional;

import com.ceos.menual.domain.expert.dto.response.ExpertPortfolioResponseDTO;
import com.ceos.menual.domain.expert.dto.response.ExpertRankingResponseDTO;
import com.ceos.menual.domain.expert.dto.response.ExpertSummaryResponseDTO;
import com.ceos.menual.entity.ExpertBankAccount;
import com.ceos.menual.entity.ExpertLike;
import com.ceos.menual.entity.ExpertProfile;
import com.ceos.menual.entity.Portfolio;
import com.ceos.menual.entity.enums.Category;

public interface ExpertRepository {
	List<ExpertRankingResponseDTO> findTop3Overall();
	List<ExpertRankingResponseDTO> findTop3ByCategory(Category category, Long currentUserId);
	// 전문가 조회용
	List<ExpertSummaryResponseDTO> findExpertList(Category category, int page, int size);

	public List<ExpertPortfolioResponseDTO> findPortfolioList(Long expertUserId, int page, int size);

	/**
	 * 포트폴리오 저장 (해시태그 포함)
	 */
	ExpertPortfolioResponseDTO savePortfolio(Portfolio portfolio, List<String> hashtagNames);


	/**
	 * 특정 포트폴리오 조회 (본인 소유 확인용)
	 */
	Optional<Portfolio> findPortfolioByExpertUserIdAndPortfolioId(Long expertUserId, Long portfolioId);

	/**
	 * 특정 전문가의 모든 대표 포트폴리오 해제 (Silent)
	 * 예외를 던지지 않고 무조건 false로 초기화합니다.
	 */
	void resetRepresentativePortfolio(Long expertUserId);

	Optional<ExpertBankAccount> findBankAccountByExpertProfileId(Long expertProfileId);


}
