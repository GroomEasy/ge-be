package com.ceos.menual.domain.review.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "후기 요약 응답 DTO")
public class ReviewSummaryResponseDTO {

	@Schema(description = "리뷰 ID")
	private Long reviewId;

	@Schema(description = "리뷰 작성자 닉네임")
	private String reviewerNickname;

	@Schema(description = "전문가 닉네임")
	private String expertNickname;

	@Schema(description = "전문가 프로필 이미지")
	private String expertProfileImage;

	@Schema(description = "전문가 평균 평점")
	private Double expertRatingAverage;

	@Schema(description = "별점")
	private Integer rating;

	@Schema(description = "내용")
	private String content;

	@Schema(description = "이미지 URL")
	private List<String> mediaUrls;

	@Schema(description = "카테고리")
	private String category;

	@Schema(description = "작성 시간")
	private String createdAt;
}
