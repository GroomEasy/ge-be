package com.ceos.menual.domain.expert.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "인기 전문가 TOP3 응답 DTO")
public class PopularExpertsResponseDTO {

	@Schema(description = "인기 전문가 TOP3 목록")
	private List<ExpertRankingResponseDTO> top3;
}
