package com.ceos.menual.domain.review.controller;

import java.util.List;

import com.ceos.menual.domain.review.dto.request.CreateReviewRequestDTO;
import com.ceos.menual.domain.review.dto.response.AvailableReviewResponseDTO;
import com.ceos.menual.domain.review.dto.response.CompletedReviewResponseDTO;
import com.ceos.menual.domain.review.dto.response.CreateReviewResponseDTO;
import com.ceos.menual.entity.enums.Category;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.ceos.menual.domain.common.dto.response.CommonResponse;
import com.ceos.menual.domain.review.dto.response.ReviewSummaryResponseDTO;
import com.ceos.menual.domain.review.service.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
@Validated
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
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "5") @Min(1) @Max(100) int size  // 기본 5개
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

	/**
	 * 작성 가능한 후기 목록 조회 API
	 */
	@Operation(
			summary = "작성 가능한 후기 목록 조회",
			description = "작성 가능한 후기 목록을 조회합니다. (reviewWritten = false)"
	)
	@GetMapping("/available")
	public ResponseEntity<CommonResponse<List<AvailableReviewResponseDTO>>> getAvailableReviews(
			@AuthenticationPrincipal Long userId
	) {
		List<AvailableReviewResponseDTO> response = reviewService.getAvailableReviews(userId);
		return ResponseEntity.ok(CommonResponse.success(response));
	}

	/**
	 * 작성 완료된 후기 목록 조회 API
	 */
	@Operation(
			summary = "작성 완료된 후기 목록 조회",
			description = "작성 완료된 후기 목록을 조회합니다. (reviewWritten = true)"
	)
	@GetMapping("/completed")
	public ResponseEntity<CommonResponse<List<CompletedReviewResponseDTO>>> getCompletedReviews(
			@AuthenticationPrincipal Long userId
	) {
		List<CompletedReviewResponseDTO> response = reviewService.getCompletedReviews(userId);
		return ResponseEntity.ok(CommonResponse.success(response));
	}

	/**
	 * 후기 작성 API
	 */
	@Operation(
			summary = "후기 작성",
			description = "상담에 대한 후기를 작성합니다."
	)
	@PostMapping
	public ResponseEntity<CommonResponse<CreateReviewResponseDTO>> createReview(
			@AuthenticationPrincipal Long userId,
			@Valid @RequestBody CreateReviewRequestDTO request
	) {
		CreateReviewResponseDTO response = reviewService.createReview(userId, request);
		return ResponseEntity.ok(CommonResponse.success(response));
	}

}
