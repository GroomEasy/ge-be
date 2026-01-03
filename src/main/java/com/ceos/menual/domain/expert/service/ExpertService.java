package com.ceos.menual.domain.expert.service;

import java.util.List;

import com.ceos.menual.domain.expert.dto.response.ExpertInfoResponseDTO;
import com.ceos.menual.domain.expert.dto.response.ExpertSummaryResponseDTO;
import com.ceos.menual.domain.expert.exception.ExpertErrorCode;
import com.ceos.menual.domain.expert.repository.ExpertLikeRepository;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.ExpertProfile;
import com.ceos.menual.entity.User;
import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.global.exception.GlobalException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ceos.menual.domain.expert.dto.response.ExpertRankingResponseDTO;
import com.ceos.menual.domain.expert.dto.response.PopularExpertsResponseDTO;
import com.ceos.menual.domain.expert.repository.ExpertRepository;

import lombok.RequiredArgsConstructor;

import static com.ceos.menual.entity.QExpertProfile.expertProfile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpertService {

	private final ExpertRepository expertRepository;
	private final UserRepository userRepository;
	private final ExpertLikeRepository expertLikeRepository;

	public PopularExpertsResponseDTO getTop3Overall() {
		List<ExpertRankingResponseDTO> top3 = expertRepository.findTop3Overall();
		return new PopularExpertsResponseDTO(top3);
	}

	public PopularExpertsResponseDTO getTop3ByCategory(Category category) {
		List<ExpertRankingResponseDTO> top3 = expertRepository.findTop3ByCategory(category);
		return new PopularExpertsResponseDTO(top3);
	}

	/**
	 * 전문가 조회
	 */
	public List<ExpertSummaryResponseDTO> getExpertList(Category category, int page, int size) {
		return expertRepository.findExpertList(category, page, size);
	}

	/**
	 * 전문가 정보 조회
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
}
