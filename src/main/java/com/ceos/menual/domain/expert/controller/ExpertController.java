package com.ceos.menual.domain.expert.controller;

import com.ceos.menual.domain.consultation.dto.response.ConsultationScheduleResponseDTO;
import com.ceos.menual.domain.expert.dto.request.AvailableScheduleUpdateRequestDTO;
import com.ceos.menual.domain.expert.dto.request.ConsultationScheduleUpdateRequestDTO;
import com.ceos.menual.domain.expert.dto.request.PortfolioCreateRequestDTO;
import com.ceos.menual.domain.expert.dto.request.PortfolioUpdateRequestDTO;
import com.ceos.menual.domain.expert.dto.request.SetRepresentativePortfolioRequestDTO;
import com.ceos.menual.domain.expert.dto.request.UpdateExpertInfoRequestDTO;
import com.ceos.menual.domain.expert.dto.request.UpdateExpertImagesRequestDTO;
import com.ceos.menual.domain.expert.dto.response.*;
import com.ceos.menual.domain.reservation.dto.response.AvailableTimesResponseDTO;
import com.ceos.menual.domain.expert.service.ExpertLikeService;
import com.ceos.menual.domain.review.dto.response.ReviewSummaryResponseDTO;
import com.ceos.menual.domain.review.service.ReviewService;
import com.ceos.menual.entity.enums.Category;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.ceos.menual.domain.common.dto.response.CommonResponse;
import com.ceos.menual.domain.expert.service.ExpertService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/expert")
@RequiredArgsConstructor
public class ExpertController {

	private final ExpertService expertService;
	private final ExpertLikeService expertLikeService;
	private final ReviewService reviewService;

//	API 폐기
//	/**
//	 * 전체 카테고리의 인기 전문가 TOP3 조회 API
//	 */
//	@Operation(
//		summary = "전체 인기 전문가 TOP3 조회",
//		description = "전체 카테고리에서 상담 완료 수 기준 TOP3 전문가를 조회합니다."
//	)
//	@GetMapping("/popular")
//	public ResponseEntity<CommonResponse<PopularExpertsResponseDTO>> getTop3Overall(){
//		PopularExpertsResponseDTO response = expertService.getTop3Overall();
//		return ResponseEntity.ok(CommonResponse.success(response));
//	}

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
			@PathVariable("category") Category category,
			@Parameter(hidden = true)
			@AuthenticationPrincipal Long currentUserId
	){
		PopularExpertsResponseDTO response = expertService.getTop3ByCategory(category, currentUserId);
		return ResponseEntity.ok(CommonResponse.success(response));
	}

	/**
	 * 전문가 조회 API
	 */
	@Operation(
			summary = "전문가 조회",
			description = "전문가 목록을 조회합니다."
	)
	@GetMapping
	public ResponseEntity<CommonResponse<List<ExpertSummaryResponseDTO>>> getExpertList(
			@Parameter(description = "전문가 카테고리")
			@RequestParam(required = false) Category category,

			@Parameter(description = "페이지 번호 (0부터 시작)")
			@RequestParam(defaultValue = "0") @Min(0) int page,

			@Parameter(description = "페이지 크기")
			@RequestParam(defaultValue = "3") @Min(1) @Max(100) int size
	) {
		List<ExpertSummaryResponseDTO> response = expertService.getExpertList(category, page, size);
		return ResponseEntity.ok(CommonResponse.success(response));
	}

	/**
	 * 전문가 상세 정보 조회 API
	 */
	@Operation(
			summary = "전문가 상세 정보 조회",
			description = "특정 전문가의 상세 정보를 조회합니다."
	)
	@GetMapping("/{userId}")
	public ResponseEntity<CommonResponse<ExpertInfoResponseDTO>> getExpertInfo(
			@Parameter(description = "전문가 UserId", required = true)
			@PathVariable Long userId
	) {
		ExpertInfoResponseDTO response = expertService.getExpertInfo(userId);
		return ResponseEntity.ok(CommonResponse.success(response));
	}

	/**
	 * 전문가별 후기 조회 API
	 */
	@Operation(
			summary = "전문가별 후기 조회",
			description = "특정 전문가의 후기 목록을 조회합니다."
	)
	@GetMapping("/{userId}/reviews")
	public ResponseEntity<CommonResponse<List<ReviewSummaryResponseDTO>>> getExpertReviews(
			@Parameter(description = "전문가 UserId", required = true)
			@PathVariable Long userId,

			@Parameter(description = "페이지 번호 (0부터 시작)")
			@RequestParam(defaultValue = "0") @Min(0) int page,

			@Parameter(description = "페이지 크기")
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
	) {
		List<ReviewSummaryResponseDTO> response = reviewService.getExpertReviews(userId, page, size);
		return ResponseEntity.ok(CommonResponse.success(response));
	}

	/**
	 * 전문가 본인 프로필/배경 이미지 수정
	 */
	@Operation(
			summary = "전문가 프로필/배경 이미지 수정",
			description = "전문가가 본인의 프로필 이미지(User.profileImage)와 배경 이미지(ExpertProfile.backgroundImage)를 수정합니다. " +
					"Presigned URL로 final 경로에 직접 업로드한 뒤(final key), 해당 key를 전달하면 서버가 DB에 공개 URL을 저장합니다."
	)
	@PutMapping("/me/images")
	public ResponseEntity<CommonResponse<ExpertInfoResponseDTO>> updateExpertImages(
			@Parameter(hidden = true)
			@AuthenticationPrincipal Long expertUserId,
			@Valid @RequestBody UpdateExpertImagesRequestDTO requestDTO
	) {
		ExpertInfoResponseDTO response = expertService.updateExpertImages(expertUserId, requestDTO);
		return ResponseEntity.ok(CommonResponse.success(response));
	}

	@Operation(
			summary = "전문가 소개서 정보 수정",
			description = "전문가가 본인의 한 줄 소개/인스타그램 링크/경력 정보를 수정합니다."
	)
	@PutMapping("/me/info")
	public ResponseEntity<CommonResponse<ExpertInfoResponseDTO>> updateExpertInfo(
			@Parameter(hidden = true)
			@AuthenticationPrincipal Long expertUserId,
			@Valid @RequestBody UpdateExpertInfoRequestDTO requestDTO
	) {
		ExpertInfoResponseDTO response = expertService.updateExpertInfo(expertUserId, requestDTO);
		return ResponseEntity.ok(CommonResponse.success(response));
	}

	/**
	 * 전문가 찜하기 API
	 */
	@Operation(
			summary = "전문가 찜하기",
			description = "특정 전문가를 찜 목록에 추가합니다."
	)
	@PostMapping("/{userId}/like")
	public ResponseEntity<CommonResponse<Void>> likeExpert(
			@Parameter(description = "찜할 전문가의 UserId", required = true)
			@PathVariable Long userId,
			@Parameter(hidden = true) @AuthenticationPrincipal Long currentUserId
	) {
		expertLikeService.likeExpert(currentUserId, userId);
		return ResponseEntity.ok(CommonResponse.success(null));
	}

	/**
	 * 전문가 찜 취소 API
	 */
	@Operation(
			summary = "전문가 찜 취소",
			description = "찜한 전문가를 찜 목록에서 제거합니다."
	)
	@DeleteMapping("/{userId}/like")
	public ResponseEntity<CommonResponse<Void>> unlikeExpert(
			@Parameter(description = "찜 취소할 전문가의 UserId", required = true)
			@PathVariable Long userId,
			@Parameter(hidden = true) @AuthenticationPrincipal Long currentUserId
	) {
		expertLikeService.unlikeExpert(currentUserId, userId);
		return ResponseEntity.ok(CommonResponse.success(null));
	}

	/**
	 * 내가 찜한 전문가 목록 조회 API
	 */
	@Operation(
			summary = "내가 찜한 전문가 목록 조회",
			description = "현재 유저가 찜한 전문가 목록을 조회합니다."
	)
	@GetMapping("/likes")
	public ResponseEntity<CommonResponse<List<ExpertSummaryResponseDTO>>> getLikedExperts(
			@Parameter(description = "카테고리 (없으면 전체 조회)")
			@RequestParam(required = false) Category category,

			@Parameter(description = "페이지 번호 (0부터 시작)")
			@RequestParam(defaultValue = "0") @Min(0) int page,

			@Parameter(description = "페이지 크기")
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,

			@Parameter(hidden = true) @AuthenticationPrincipal Long currentUserId
	) {
		List<ExpertSummaryResponseDTO> response = expertLikeService.getLikedExperts(currentUserId, category, page, size);
		return ResponseEntity.ok(CommonResponse.success(response));
	}

	/**
	 * 전문가 상담 스케줄 목록 조회 API
	 */
	@Operation(
			summary = "전문가 상담 스케줄 목록 조회",
			description = "특정 전문가가 제공하는 상담 스케줄 목록을 조회합니다."

	)
	@GetMapping("/{userId}/schedules")
	public ResponseEntity<CommonResponse<List<ConsultationScheduleResponseDTO>>> getConsultationSchedules(
			@Parameter(
					description = "전문가 UserId",
					required = true,
					example = "3"
			)
			@PathVariable Long userId
	) {
		List<ConsultationScheduleResponseDTO> response = expertService.getConsultationSchedules(userId);
		return ResponseEntity.ok(CommonResponse.success(response));
	}

	/**
	 * 전문가 상담 스케줄 수정(등록) API
	 */
	@Operation(
			summary = "전문가 상담 스케줄 수정",
			description = "전문가가 자신의 상담 스케줄(가격, 활성화 여부)을 수정합니다. 기존 스케줄이 없으면 새로 생성됩니다."
	)
	@PutMapping("/schedules")
	public ResponseEntity<CommonResponse<List<ConsultationScheduleResponseDTO>>> updateConsultationSchedules(
			@Parameter(hidden = true)
			@AuthenticationPrincipal Long expertUserId,

			@Valid @RequestBody ConsultationScheduleUpdateRequestDTO requestDTO
	) {
		List<ConsultationScheduleResponseDTO> response = expertService.updateConsultationSchedules(expertUserId, requestDTO);
		return ResponseEntity.ok(CommonResponse.success(response));
	}

	/**
	 * 전문가 예약 가능 시간 수정(등록) API
	 */
	@Operation(
			summary = "전문가 예약 가능 시간 수정",
			description = "전문가가 특정 날짜의 예약 가능 시간을 설정합니다. 요청에 포함된 시간만 활성화되고, 기존에 등록된 다른 시간은 비활성화됩니다."
	)
	@PutMapping("/available-schedules")
	public ResponseEntity<CommonResponse<AvailableTimesResponseDTO>> updateAvailableSchedules(
			@Parameter(hidden = true)
			@AuthenticationPrincipal Long expertUserId,

			@Valid @RequestBody AvailableScheduleUpdateRequestDTO requestDTO
	) {
		AvailableTimesResponseDTO response = expertService.updateAvailableSchedules(expertUserId, requestDTO);
		return ResponseEntity.ok(CommonResponse.success(response));
	}

	/**
	 * 전문가 포트폴리오 조회
	 */
	@Operation(
			summary = "전문가 포트폴리오 목록 조회",
			description = "특정 전문가의 포트폴리오 목록을 페이징 방식으로 조회합니다. 대표 포트폴리오가 우선 노출됩니다."
	)
	@GetMapping("/{userId}/portfolios")
	public ResponseEntity<CommonResponse<List<ExpertPortfolioResponseDTO>>> getPortfolios(
			@Parameter(description = "전문가 UserId", required = true)
			@PathVariable Long userId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "3") int size) {

		List<ExpertPortfolioResponseDTO> response = expertService.getPortfolioList(userId, page, size);
		return ResponseEntity.ok(CommonResponse.success(response));
	}

	/**
	 * 포트폴리오 등록
	 */
	@Operation(
			summary = "포트폴리오 등록",
			description = "전문가가 새로운 포트폴리오를 등록합니다."
	)
	@PostMapping("/portfolios")
	public ResponseEntity<CommonResponse<ExpertPortfolioResponseDTO>> createPortfolio(
			@Parameter(hidden = true)
			@AuthenticationPrincipal Long expertUserId,

			@Valid @RequestBody PortfolioCreateRequestDTO requestDTO
	) {
		ExpertPortfolioResponseDTO response = expertService.createPortfolio(expertUserId, requestDTO);
		return ResponseEntity.ok(CommonResponse.success(response));
	}

	@Operation(
			summary = "포트폴리오 수정",
			description = "전문가가 본인의 포트폴리오를 수정합니다. 변경할 필드만 전달하면 됩니다."
	)
	@PutMapping("/portfolios/{portfolioId}")
	public ResponseEntity<CommonResponse<ExpertPortfolioResponseDTO>> updatePortfolio(
			@Parameter(hidden = true)
			@AuthenticationPrincipal Long expertUserId,
			@PathVariable Long portfolioId,
			@Valid @RequestBody PortfolioUpdateRequestDTO requestDTO
	) {
		ExpertPortfolioResponseDTO response = expertService.updatePortfolio(expertUserId, portfolioId, requestDTO);
		return ResponseEntity.ok(CommonResponse.success(response));
	}

	@Operation(
			summary = "포트폴리오 삭제",
			description = "전문가가 본인의 포트폴리오를 삭제합니다."
	)
	@DeleteMapping("/portfolios/{portfolioId}")
	public ResponseEntity<CommonResponse<Void>> deletePortfolio(
			@Parameter(hidden = true)
			@AuthenticationPrincipal Long expertUserId,
			@PathVariable Long portfolioId
	) {
		expertService.deletePortfolio(expertUserId, portfolioId);
		return ResponseEntity.ok(CommonResponse.success(null));
	}

	/**
	 * 대표 포트폴리오 토글 (설정/해제)
	 */
	@Operation(
			summary = "대표 포트폴리오 토글",
			description = "특정 포트폴리오를 대표로 설정하거나 해제합니다. (이미 대표면 해제, 아니면 다른 대표 해제 후 설정)"
	)
	@PatchMapping("/portfolios/representative")
	public ResponseEntity<CommonResponse<ToggleRepresentativePortfolioResponseDTO>> toggleRepresentativePortfolio(
			@Parameter(hidden = true)
			@AuthenticationPrincipal Long expertUserId,

			@Valid @RequestBody SetRepresentativePortfolioRequestDTO requestDTO
	) {
		// Service 호출 및 DTO 수신
		ToggleRepresentativePortfolioResponseDTO response =
				expertService.toggleRepresentativePortfolio(expertUserId, requestDTO);

		return ResponseEntity.ok(CommonResponse.success(response));
	}

}
