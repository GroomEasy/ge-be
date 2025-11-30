package com.ceos.menual.domain.review.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "후기 요약 응닫 DTO")
public class ReviewSummaryResponseDTO {

	@Schema(description = "리뷰 ID")
	private Long reviewId;

	@Schema(description = "별점")
	private Integer rating;

	@Schema(description = "내용")
	private String content;

	@Schema(description = "이미지 URL")
	private String mediaUrls;

	@Schema(description = "좋아요 수")
	private Integer likeCount;

	@Schema(description = "카테고리 ID")
	private Long categoryId;

	@Schema(description = "작성 시간")
	private String createdAt;
}
