package com.ceos.menual.domain.expert.service;

import java.util.List;
import java.util.stream.Collectors;

import com.ceos.menual.domain.consultation.dto.response.ConsultationScheduleResponseDTO;
import com.ceos.menual.domain.consultation.repository.ConsultationScheduleRepository;
import com.ceos.menual.domain.expert.dto.response.ExpertInfoResponseDTO;
import com.ceos.menual.domain.expert.dto.response.ExpertSummaryResponseDTO;
import com.ceos.menual.domain.expert.exception.ExpertErrorCode;
import com.ceos.menual.domain.expert.repository.ExpertLikeRepository;
import com.ceos.menual.domain.reservation.exception.ReservationErrorCode;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.ConsultationSchedule;
import com.ceos.menual.entity.ExpertProfile;
import com.ceos.menual.entity.User;
import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.entity.enums.UserType;
import com.ceos.menual.global.exception.GlobalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ceos.menual.domain.expert.dto.response.ExpertRankingResponseDTO;
import com.ceos.menual.domain.expert.dto.response.PopularExpertsResponseDTO;
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

	public PopularExpertsResponseDTO getTop3ByCategory(Category category) {
		List<ExpertRankingResponseDTO> top3 = expertRepository.findTop3ByCategory(category);
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
	public List<ConsultationScheduleResponseDTO> getConsultationSchedules(Long UserId) {
		log.info("상담 스케줄 목록 조회 - 전문가 UserId: {}", UserId);

		// 전문가 조회 및 검증
		User expertUser = userRepository.findById(UserId)
				.orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

		if (expertUser.getUserType() != UserType.EXPERT || expertUser.getExpertProfile() == null) {
			throw new GlobalException(ReservationErrorCode.EXPERT_PROFILE_NOT_FOUND);
		}

		Long expertProfileId = expertUser.getExpertProfile().getId();

		// 활성화된 상담 스케줄 목록 조회
		List<ConsultationSchedule> schedules = consultationScheduleRepository
				.findActiveSchedulesByExpertProfileId(expertProfileId);

		log.info("상담 스케줄 조회 완료 - 전문가 UserId: {}, 스케줄 개수: {}", UserId, schedules.size());

		return schedules.stream()
				.map(ConsultationScheduleResponseDTO::from)
				.collect(Collectors.toList());
	}
}
