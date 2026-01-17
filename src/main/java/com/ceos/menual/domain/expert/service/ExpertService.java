package com.ceos.menual.domain.expert.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.ceos.menual.domain.consultation.dto.response.ConsultationScheduleResponseDTO;
import com.ceos.menual.domain.consultation.repository.ConsultationScheduleRepository;
import com.ceos.menual.domain.expert.dto.request.PortfolioCreateRequestDTO;
import com.ceos.menual.domain.expert.dto.request.SetRepresentativePortfolioRequestDTO;
import com.ceos.menual.domain.expert.dto.response.*;
import com.ceos.menual.domain.expert.exception.ExpertErrorCode;
import com.ceos.menual.domain.expert.repository.ExpertLikeRepository;
import com.ceos.menual.domain.reservation.exception.ReservationErrorCode;
import com.ceos.menual.domain.review.repository.ReviewRepository;
import com.ceos.menual.domain.user.dto.request.ExpertConversionRequestDTO;
import com.ceos.menual.domain.user.dto.response.ExpertConversionResponseDTO;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.ExpertProfileRepository;
import com.ceos.menual.domain.user.repository.GeneralProfileRepository;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.*;
import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.entity.enums.UserType;
import com.ceos.menual.global.exception.GlobalException;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ceos.menual.domain.expert.repository.ExpertRepository;

import lombok.RequiredArgsConstructor;

import static com.ceos.menual.entity.QExpertProfile.expertProfile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpertService {

	private final ExpertRepository expertRepository;
	private final UserRepository userRepository;
	private final ExpertLikeRepository expertLikeRepository;
	private final ConsultationScheduleRepository consultationScheduleRepository;


	public PopularExpertsResponseDTO getTop3Overall() {
		List<ExpertRankingResponseDTO> top3 = expertRepository.findTop3Overall();
		return new PopularExpertsResponseDTO(top3);
	}

	public PopularExpertsResponseDTO getTop3ByCategory(Category category, Long currentUserId) {
		List<ExpertRankingResponseDTO> top3 = expertRepository.findTop3ByCategory(category, currentUserId);
		return new PopularExpertsResponseDTO(top3);
	}

	/**
	 * 전문가 조회 API
	 */
	public List<ExpertSummaryResponseDTO> getExpertList(Category category, int page, int size) {
		return expertRepository.findExpertList(category, page, size);
	}

	/**
	 * 전문가 정보 조회 API
	 */
	public ExpertInfoResponseDTO getExpertInfo(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

		// 전문가 여부 확인
		if (!user.isExpert()) {
			throw new GlobalException(ExpertErrorCode.USER_NOT_EXPERT);
		}

		ExpertProfile expertProfile = user.getExpertProfile();

		Long likeCount = expertLikeRepository.countByExpertProfileId(expertProfile.getId());

		return ExpertInfoResponseDTO.from(user, likeCount.intValue());
	}

	/**
	 * 전문가가 제공하는 상담 스케줄 목록 조회 API
	 */
	public List<ConsultationScheduleResponseDTO> getConsultationSchedules(Long userId) {
		log.info("상담 스케줄 목록 조회 - 전문가 UserId: {}", userId);

		// 전문가 조회 및 검증
		User expertUser = userRepository.findById(userId)
				.orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

		if (!expertUser.isExpert() || expertUser.getExpertProfile() == null) {
			throw new GlobalException(ExpertErrorCode.EXPERT_PROFILE_NOT_FOUND);
		}

		Long expertProfileId = expertUser.getExpertProfile().getId();

		// 활성화된 상담 스케줄 목록 조회
		List<ConsultationSchedule> schedules = consultationScheduleRepository
				.findActiveSchedulesByExpertProfileId(expertProfileId);

		log.info("상담 스케줄 조회 완료 - 전문가 UserId: {}, 스케줄 개수: {}", userId, schedules.size());

		return schedules.stream()
				.map(ConsultationScheduleResponseDTO::from)
				.collect(Collectors.toList());
	}

	/**
	 * 전문가 포트폴리오 조회
	 */
	public List<ExpertPortfolioResponseDTO> getPortfolioList(Long expertUserId, int page, int size) {
		// 전문가 조회 및 검증
		User user = userRepository.findById(expertUserId)
				.orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

		// 전문가 여부 확인
		if (!user.isExpert()) {
			throw new GlobalException(ExpertErrorCode.USER_NOT_EXPERT);
		}

		return expertRepository.findPortfolioList(expertUserId, page, size);
	}

	/**
	 * 포트폴리오 등록
	 */
	@Transactional
	public ExpertPortfolioResponseDTO createPortfolio(Long expertUserId, PortfolioCreateRequestDTO requestDTO) {
		// 전문가 조회 및 검증
		User user = userRepository.findById(expertUserId)
				.orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

		if (!user.isExpert()) {
			throw new GlobalException(ExpertErrorCode.USER_NOT_EXPERT);
		}

		ExpertProfile expertProfile = user.getExpertProfile();
		if (expertProfile == null) {
			throw new GlobalException(ExpertErrorCode.EXPERT_PROFILE_NOT_FOUND);
		}

		// Portfolio 엔티티 생성
		Portfolio portfolio = Portfolio.builder()
				.expertProfile(expertProfile)
				.title(requestDTO.getTitle())
				.concern(requestDTO.getConcern())
				.solution(requestDTO.getSolution())
				.beforeImage(requestDTO.getBeforeImage())
				.afterImage(requestDTO.getAfterImage())
				.build();

		// 저장 및 반환
		return expertRepository.savePortfolio(portfolio, requestDTO.getHashtags());
	}

	/**
	 * 대표 포트폴리오 토글 (설정/해제)
	 */
	@Transactional
	public ToggleRepresentativePortfolioResponseDTO toggleRepresentativePortfolio(Long expertUserId, SetRepresentativePortfolioRequestDTO requestDTO) {
		log.info("대표 포트폴리오 토글 요청 - userId: {}, portfolioId: {}", expertUserId, requestDTO.getPortfolioId());

		// 전문가 검증
		User user = userRepository.findById(expertUserId)
				.orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

		if (!user.isExpert()) {
			throw new GlobalException(ExpertErrorCode.USER_NOT_EXPERT);
		}

		// 대상 포트폴리오 조회 (본인 소유 확인)
		Portfolio portfolio = expertRepository.findPortfolioByExpertUserIdAndPortfolioId(
				expertUserId,
				requestDTO.getPortfolioId()
		).orElseThrow(() -> new GlobalException(ExpertErrorCode.PORTFOLIO_NOT_FOUND));

		boolean finalState;

		if (portfolio.isRepresentative()) {
			// 대표 -> 해제 (False)
			portfolio.setIsRepresentative(false);
			finalState = false;
		} else {
			// 대표 아님 -> 기존 것들 모두 해제 후 -> 현재 것 설정 (True)

			// 기존 대표 초기화
			expertRepository.resetRepresentativePortfolio(expertUserId);

			// 영속성 컨텍스트가 비워졌으므로 포트폴리오를 다시 조회해야 함 (Re-fetch)
			Portfolio targetPortfolio = expertRepository.findPortfolioByExpertUserIdAndPortfolioId(
					expertUserId,
					requestDTO.getPortfolioId()
			).orElseThrow(() -> new GlobalException(ExpertErrorCode.PORTFOLIO_NOT_FOUND));

			// 대표 설정
			targetPortfolio.setIsRepresentative(true);
			finalState = true;
		}

		log.info("대표 포트폴리오 토글 완료 - portfolioId: {}, finalState: {}", requestDTO.getPortfolioId(), finalState);

		// 결과 DTO 반환
		return ToggleRepresentativePortfolioResponseDTO.builder()
				.portfolioId(requestDTO.getPortfolioId())
				.isRepresentative(finalState)
				.build();
	}
}
