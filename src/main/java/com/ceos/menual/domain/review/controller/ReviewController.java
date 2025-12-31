package com.ceos.menual.domain.review.controller;

import java.util.List;

import com.ceos.menual.entity.enums.Category;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ceos.menual.domain.common.dto.response.CommonResponse;
import com.ceos.menual.domain.review.dto.response.ReviewSummaryResponseDTO;
import com.ceos.menual.domain.review.service.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

	private final ReviewService reviewService;

	/*
	 * 카테고리별 실시간 후기 조회 API
	 * (parameter 없으면 전체에서 조회)
	 */
	@Operation(summary = "실시간 후기 조회", description = "최신 순으로 후기 목록을 조회합니다.")
	@GetMapping("/recent")
	public ResponseEntity<CommonResponse<List<ReviewSummaryResponseDTO>>> getRecentReviews(
			@RequestParam(required = false) Category category,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size  // 기본 5개
	) {
		List<ReviewSummaryResponseDTO> response = reviewService.getRecentReviews(category, page, size);
		return ResponseEntity.ok(CommonResponse.success(response));
	}

	/*
	 * 카테고리별 베스트 후기 조회 API
	 * (parameter 없으면 전체에서 조회)
	 */
	@Operation(summary = "베스트 후기 조회", description = "좋아요 수가 많은 후기 목록을 조회합니다.")
	@GetMapping("/best")
	public ResponseEntity<CommonResponse<List<ReviewSummaryResponseDTO>>> getBestReviews(
		@RequestParam(required = false) Category category
	){
		List<ReviewSummaryResponseDTO> response = reviewService.getBestReviews(category);
		return ResponseEntity.ok(CommonResponse.success(response));
	}

}
