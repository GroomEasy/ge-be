package com.ceos.menual.domain.expert.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ceos.menual.domain.expert.dto.response.ExpertRankingResponseDTO;
import com.ceos.menual.domain.expert.dto.response.PopularExpertsResponseDTO;
import com.ceos.menual.domain.expert.repository.ExpertRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpertService {

	private final ExpertRepository expertRepository;

	public PopularExpertsResponseDTO getTop3Overall() {
		List<ExpertRankingResponseDTO> top3 = expertRepository.findTop3Overall();
		return new PopularExpertsResponseDTO(top3);
	}

	public PopularExpertsResponseDTO getTop3ByCategory(Long categoryId) {
		List<ExpertRankingResponseDTO> top3 = expertRepository.findTop3ByCategory(categoryId);
		return new PopularExpertsResponseDTO(top3);
	}
}
