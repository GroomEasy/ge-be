package com.ceos.menual.domain.expert.controller;

import com.ceos.menual.entity.enums.Category;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ceos.menual.domain.common.dto.response.CommonResponse;
import com.ceos.menual.domain.expert.dto.response.PopularExpertsResponseDTO;
import com.ceos.menual.domain.expert.service.ExpertService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/expert")
@RequiredArgsConstructor
public class ExpertController {

	private final ExpertService expertService;

	/**
	 * 전체 카테고리의 인기 전문가 TOP3 조회 API
	 */
	@Operation(
		summary = "전체 인기 전문가 TOP3 조회",
		description = "전체 카테고리에서 상담 완료 수 기준 TOP3 전문가를 조회합니다."
	)
	@GetMapping("/popular")
	public ResponseEntity<CommonResponse<PopularExpertsResponseDTO>> getTop3Overall(){
		PopularExpertsResponseDTO response = expertService.getTop3Overall();
		return ResponseEntity.ok(CommonResponse.success(response));
	}

	/**
	 * 카테고리별 인기 전문가 TOP3 조회 API
	 */
	@Operation(
		summary = "카테고리별 인기 전문가 TOP3 조회",
		description = "카테고리별로 상담 완료 수 기준 TOP3 전문가를 조회합니다."
	)
	@GetMapping("/popular/{category}")
	public ResponseEntity<CommonResponse<PopularExpertsResponseDTO>> getTop3ByCategory(
			@Parameter(description = "카테고리명 (HAIR, FASHION, SKIN, MAKEUP 중 택1)", required = true)
			@PathVariable("category") Category category
	){
		PopularExpertsResponseDTO response = expertService.getTop3ByCategory(category);
		return ResponseEntity.ok(CommonResponse.success(response));
	}

	/**
	 * 카테고리별 전문가 조회 API
	 */

//	@GetMapping("/")

}
