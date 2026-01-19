package com.ceos.menual.domain.expert.service;

import java.util.*;
import java.util.stream.Collectors;

import com.ceos.menual.domain.consultation.dto.response.ConsultationScheduleResponseDTO;
import com.ceos.menual.domain.consultation.repository.ConsultationScheduleRepository;
import com.ceos.menual.domain.common.service.S3PresignedUrlService;
import com.ceos.menual.domain.expert.dto.request.AvailableScheduleUpdateRequestDTO;
import com.ceos.menual.domain.expert.dto.request.ConsultationScheduleUpdateRequestDTO;
import com.ceos.menual.domain.expert.dto.request.PortfolioCreateRequestDTO;
import com.ceos.menual.domain.expert.dto.request.SetRepresentativePortfolioRequestDTO;
import com.ceos.menual.domain.expert.dto.request.UpdateExpertImagesRequestDTO;
import com.ceos.menual.domain.expert.dto.response.*;
import com.ceos.menual.domain.expert.exception.ExpertErrorCode;
import com.ceos.menual.domain.expert.repository.ExpertLikeRepository;
import com.ceos.menual.domain.reservation.dto.response.AvailableTimesResponseDTO;
import com.ceos.menual.domain.reservation.exception.ReservationErrorCode;
import com.ceos.menual.domain.reservation.repository.AvailableScheduleRepository;
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
	private final AvailableScheduleRepository availableScheduleRepository;
	private final S3PresignedUrlService s3PresignedUrlService;


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
	 * 전문가 본인 프로필/배경 이미지 수정
	 *
	 * - 입력: S3 final key (final/expert/{expertId}/profile|background/{fileName})
	 * - 처리: DB에는 공개 URL 저장 (이전 이미지가 있으면 best-effort로 삭제 시도)
	 */
	@Transactional
	public ExpertInfoResponseDTO updateExpertImages(Long expertUserId, UpdateExpertImagesRequestDTO requestDTO) {
		User user = userRepository.findById(expertUserId)
			.orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

		if (!user.isExpert() || user.getExpertProfile() == null) {
			throw new GlobalException(ExpertErrorCode.USER_NOT_EXPERT);
		}

		ExpertProfile expertProfile = user.getExpertProfile();

		if (requestDTO.getProfileImageKey() != null && !requestDTO.getProfileImageKey().trim().isEmpty()) {
			String newProfileFinalKey = validateExpertFinalKey(expertUserId, requestDTO.getProfileImageKey(), "profile");
			String oldProfileKey = extractS3KeyFromStoredUrl(user.getProfileImage());
			if (oldProfileKey != null && oldProfileKey.startsWith("final/expert/")) {
				s3PresignedUrlService.deleteObjectWithRetryOrEnqueue(oldProfileKey);
			}
			user.updateProfileImage(s3PresignedUrlService.generateS3Url(newProfileFinalKey));
		}

		if (requestDTO.getBackgroundImageKey() != null && !requestDTO.getBackgroundImageKey().trim().isEmpty()) {
			String newBackgroundFinalKey = validateExpertFinalKey(expertUserId, requestDTO.getBackgroundImageKey(), "background");
			String oldBackgroundKey = extractS3KeyFromStoredUrl(expertProfile.getBackgroundImage());
			if (oldBackgroundKey != null && oldBackgroundKey.startsWith("final/expert/")) {
				s3PresignedUrlService.deleteObjectWithRetryOrEnqueue(oldBackgroundKey);
			}
			expertProfile.updateBackgroundImage(s3PresignedUrlService.generateS3Url(newBackgroundFinalKey));
		}

		Long likeCount = expertLikeRepository.countByExpertProfileId(expertProfile.getId());
		return ExpertInfoResponseDTO.from(user, likeCount.intValue());
	}

	private String validateExpertFinalKey(Long expertUserId, String keyOrUrl, String expectedImageType) {
		String s3Key = normalizeS3KeyFromMaybeUrl(keyOrUrl);
		if (s3Key == null || s3Key.isBlank()) {
			throw new GlobalException(ExpertErrorCode.INVALID_IMAGE_KEY_FORMAT);
		}

		String expectedPrefix = "final/expert/" + expertUserId + "/";
		if (!s3Key.startsWith(expectedPrefix)) {
			log.warn("전문가 final 이미지 키 소유권 불일치 - expertUserId: {}, key: {}", expertUserId, s3Key);
			throw new GlobalException(ExpertErrorCode.INVALID_IMAGE_KEY_FORMAT);
		}

		String[] parts = s3Key.split("/");
		// final/expert/{expertId}/{imageType}/{fileName}
		if (parts.length < 5) {
			throw new GlobalException(ExpertErrorCode.INVALID_IMAGE_KEY_FORMAT);
		}

		String imageType = parts[3];
		String fileName = parts[4];
		if (!expectedImageType.equals(imageType)) {
			throw new GlobalException(ExpertErrorCode.INVALID_IMAGE_KEY_FORMAT);
		}
		if (fileName == null || fileName.isBlank()) {
			throw new GlobalException(ExpertErrorCode.INVALID_IMAGE_KEY_FORMAT);
		}

		return s3Key;
	}

	private String extractS3KeyFromStoredUrl(String storedUrlOrKey) {
		if (storedUrlOrKey == null) return null;
		String trimmed = storedUrlOrKey.trim();
		int finalIdx = trimmed.indexOf("final/");
		if (finalIdx >= 0) return trimmed.substring(finalIdx);
		return trimmed;
	}

	private String normalizeS3KeyFromMaybeUrl(String keyOrUrl) {
		if (keyOrUrl == null) {
			return null;
		}
		String trimmed = keyOrUrl.trim();
		if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
			try {
				java.net.URI uri = java.net.URI.create(trimmed);
				String path = uri.getPath();
				if (path == null) return trimmed;
				if (path.startsWith("/")) path = path.substring(1);
				return path;
			} catch (Exception e) {
				return trimmed;
			}
		}
		return trimmed;
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
		ExpertPortfolioResponseDTO created = expertRepository.savePortfolio(portfolio, requestDTO.getHashtags());

		// 이미지 이동: tmp/portfolio/expert-{expertId}/{before|after}/{fileName} -> final/portfolio/{portfolioId}/{before|after}/{fileName}
		Long portfolioId = portfolio.getId();
		MovedPortfolioImages moved = moveBothPortfolioImagesOrRollback(
				expertUserId,
				portfolioId,
				requestDTO.getBeforeImage(),
				requestDTO.getAfterImage()
		);

		portfolio.updateBeforeImage(moved.beforeFinalUrl());
		portfolio.updateAfterImage(moved.afterFinalUrl());

		return ExpertPortfolioResponseDTO.builder()
				.id(created.getId())
				.title(created.getTitle())
				.concern(created.getConcern())
				.solution(created.getSolution())
				.isRepresentative(created.getIsRepresentative())
				.beforeImage(portfolio.getBeforeImage())
				.afterImage(portfolio.getAfterImage())
				.hashtags(created.getHashtags())
				.build();
	}

	private String movePortfolioImageToFinalKey(Long expertUserId, Long portfolioId, String imageKeyOrUrl) {
		if (imageKeyOrUrl == null || imageKeyOrUrl.trim().isEmpty()) {
			throw new GlobalException(ExpertErrorCode.INVALID_IMAGE_KEY_FORMAT);
		}

		String s3Key = imageKeyOrUrl.trim();
		if (!s3Key.startsWith("tmp/portfolio/")) {
			// 키로 통일: URL/외부 URL/이미 final 키 등은 허용하지 않음
			throw new GlobalException(ExpertErrorCode.INVALID_IMAGE_KEY_FORMAT);
		}

		String expectedPrefix = "tmp/portfolio/expert-" + expertUserId + "/";
		if (!s3Key.startsWith(expectedPrefix)) {
			log.warn("포트폴리오 이미지 키 소유권 불일치 - expertUserId: {}, key: {}", expertUserId, s3Key);
			throw new GlobalException(ExpertErrorCode.INVALID_IMAGE_KEY_FORMAT);
		}

		String[] parts = s3Key.split("/");
		// tmp/portfolio/expert-{expertId}/{imageType}/{fileName}
		if (parts.length < 5) {
			log.warn("포트폴리오 이미지 키 형식 불일치 - key: {}", s3Key);
			throw new GlobalException(ExpertErrorCode.INVALID_IMAGE_KEY_FORMAT);
		}

		String imageType = parts[3]; // before|after
		String fileName = parts[4];
		if (!"before".equals(imageType) && !"after".equals(imageType)) {
			log.warn("허용되지 않는 포트폴리오 이미지 타입 - type: {}, key: {}", imageType, s3Key);
			throw new GlobalException(ExpertErrorCode.INVALID_IMAGE_KEY_FORMAT);
		}
		if (fileName == null || fileName.isBlank()) {
			throw new GlobalException(ExpertErrorCode.INVALID_IMAGE_KEY_FORMAT);
		}

		String finalS3Key = String.format("final/portfolio/%d/%s/%s", portfolioId, imageType, fileName);
		s3PresignedUrlService.moveImageFromTempToFinal(s3Key, finalS3Key);
		return finalS3Key;
	}

	private record MovedPortfolioImages(String beforeFinalUrl, String afterFinalUrl, String beforeFinalKey, String afterFinalKey) {}

	private MovedPortfolioImages moveBothPortfolioImagesOrRollback(
			Long expertUserId,
			Long portfolioId,
			String beforeTmpKey,
			String afterTmpKey
	) {
		String beforeFinalKey = null;
		String afterFinalKey = null;

		try {
			beforeFinalKey = movePortfolioImageToFinalKey(expertUserId, portfolioId, beforeTmpKey);
			afterFinalKey = movePortfolioImageToFinalKey(expertUserId, portfolioId, afterTmpKey);

			return new MovedPortfolioImages(
					s3PresignedUrlService.generateS3Url(beforeFinalKey),
					s3PresignedUrlService.generateS3Url(afterFinalKey),
					beforeFinalKey,
					afterFinalKey
			);
		} catch (Exception e) {
			// 보상 정리: 이미 옮긴 final 객체가 있으면 삭제 시도 (실패하면 cleanup task 기록)
			try {
				if (beforeFinalKey != null) {
					s3PresignedUrlService.deleteObjectWithRetryOrEnqueue(beforeFinalKey);
				}
			} catch (Exception ignore) {
				// 보상 실패는 원인 예외를 가리지 않도록 무시
			}
			try {
				if (afterFinalKey != null) {
					s3PresignedUrlService.deleteObjectWithRetryOrEnqueue(afterFinalKey);
				}
			} catch (Exception ignore) {
				// 보상 실패는 원인 예외를 가리지 않도록 무시
			}

			throw e;
		}
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

	/**
	 * 전문가 상담 스케줄 수정 (등록/업데이트)
	 */
	@Transactional
	public List<ConsultationScheduleResponseDTO> updateConsultationSchedules(
			Long expertUserId,
			ConsultationScheduleUpdateRequestDTO requestDTO
	) {
		log.info("상담 스케줄 수정 요청 - 전문가 UserId: {}", expertUserId);

		// 전문가 조회 및 검증
		User expertUser = userRepository.findById(expertUserId)
				.orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

		if (!expertUser.isExpert() || expertUser.getExpertProfile() == null) {
			throw new GlobalException(ExpertErrorCode.EXPERT_PROFILE_NOT_FOUND);
		}

		ExpertProfile profile = expertUser.getExpertProfile();
		Long expertProfileId = profile.getId();

		List<ConsultationSchedule> updatedSchedules = new ArrayList<>();

		for (ConsultationScheduleUpdateRequestDTO.ScheduleItem item : requestDTO.getSchedules()) {
			// 기존 스케줄 조회 (활성화 여부 무관)
			ConsultationSchedule schedule = consultationScheduleRepository
					.findByExpertProfileIdAndConsultationType(expertProfileId, item.getConsultationType())
					.orElse(null);

			if (schedule != null) {
				// 기존 스케줄 업데이트
				schedule.updatePrice(item.getPrice());
				if (item.getIsActive()) {
					schedule.activate();
				} else {
					schedule.deactivate();
				}
				updatedSchedules.add(schedule);
				log.info("상담 스케줄 업데이트 - type: {}, price: {}, isActive: {}",
						item.getConsultationType(), item.getPrice(), item.getIsActive());
			} else {
				// 새 스케줄 생성
				ConsultationSchedule newSchedule = ConsultationSchedule.builder()
						.consultationType(item.getConsultationType())
						.price(item.getPrice())
						.isActive(item.getIsActive())
						.build();
				profile.addConsultationSchedule(newSchedule);
				consultationScheduleRepository.save(newSchedule);
				updatedSchedules.add(newSchedule);
				log.info("상담 스케줄 생성 - type: {}, price: {}, isActive: {}",
						item.getConsultationType(), item.getPrice(), item.getIsActive());
			}
		}

		log.info("상담 스케줄 수정 완료 - 전문가 UserId: {}, 수정된 스케줄 수: {}",
				expertUserId, updatedSchedules.size());

		return updatedSchedules.stream()
				.map(ConsultationScheduleResponseDTO::from)
				.collect(Collectors.toList());
	}

	/**
	 * 전문가 예약 가능 시간 수정 (등록/업데이트)
	 * 특정 날짜의 예약 가능 시간을 설정합니다.
	 * 기존에 등록된 시간은 비활성화하고, 요청된 시간들을 활성화합니다.
	 */
	@Transactional
	public AvailableTimesResponseDTO updateAvailableSchedules(
			Long expertUserId,
			AvailableScheduleUpdateRequestDTO requestDTO
	) {
		log.info("예약 가능 시간 수정 요청 - 전문가 UserId: {}, 날짜: {}",
				expertUserId, requestDTO.getAvailableDate());

		// 전문가 조회 및 검증
		User expertUser = userRepository.findById(expertUserId)
				.orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

		if (!expertUser.isExpert() || expertUser.getExpertProfile() == null) {
			throw new GlobalException(ExpertErrorCode.EXPERT_PROFILE_NOT_FOUND);
		}

		ExpertProfile profile = expertUser.getExpertProfile();
		Long expertProfileId = profile.getId();

		// 해당 날짜의 기존 스케줄 모두 조회 (활성/비활성 모두)
		List<AvailableSchedule> existingSchedules = availableScheduleRepository
				.findAllByExpertProfileIdAndDate(expertProfileId, requestDTO.getAvailableDate());

		// 기존 스케줄을 Map으로 변환 (시간 -> 스케줄)
		Map<java.time.LocalTime, AvailableSchedule> existingScheduleMap = existingSchedules.stream()
				.collect(Collectors.toMap(
						AvailableSchedule::getAvailableTime,
						schedule -> schedule
				));

		// 요청된 시간들을 Set으로 변환
		Set<java.time.LocalTime> requestedTimes = new HashSet<>(requestDTO.getAvailableTimes());

		List<java.time.LocalTime> resultTimes = new ArrayList<>();

		// 1. 기존 스케줄 처리: 요청에 없으면 비활성화, 있으면 활성화
		for (AvailableSchedule existingSchedule : existingSchedules) {
			if (requestedTimes.contains(existingSchedule.getAvailableTime())) {
				// 요청된 시간이면 활성화
				existingSchedule.activate();
				resultTimes.add(existingSchedule.getAvailableTime());
				log.debug("기존 스케줄 활성화 - time: {}", existingSchedule.getAvailableTime());
			} else {
				// 요청에 없는 시간이면 비활성화
				existingSchedule.deactivate();
				log.debug("기존 스케줄 비활성화 - time: {}", existingSchedule.getAvailableTime());
			}
		}

		// 2. 새로운 시간 추가: 기존에 없는 시간만 생성
		for (java.time.LocalTime requestedTime : requestDTO.getAvailableTimes()) {
			if (!existingScheduleMap.containsKey(requestedTime)) {
				// 새로운 시간이면 생성
				AvailableSchedule newSchedule = AvailableSchedule.builder()
						.expertProfile(profile)
						.availableDate(requestDTO.getAvailableDate())
						.availableTime(requestedTime)
						.isActive(true)
						.build();
				availableScheduleRepository.save(newSchedule);
				resultTimes.add(requestedTime);
				log.debug("새 스케줄 생성 - time: {}", requestedTime);
			}
		}

		// 시간순 정렬
		resultTimes.sort(java.time.LocalTime::compareTo);

		log.info("예약 가능 시간 수정 완료 - 전문가 UserId: {}, 날짜: {}, 설정된 시간 수: {}",
				expertUserId, requestDTO.getAvailableDate(), resultTimes.size());

		return AvailableTimesResponseDTO.builder()
				.date(requestDTO.getAvailableDate())
				.availableTimes(resultTimes)
				.build();
	}
}
