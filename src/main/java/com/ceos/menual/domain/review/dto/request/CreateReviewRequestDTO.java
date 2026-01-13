package com.ceos.menual.domain.review.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequestDTO {

    @NotNull(message = "상담 ID는 필수입니다.")
    @Schema(description = "상담 ID", example = "1")
    private Long consultationId;

    @NotNull(message = "평점은 필수입니다.")
    @Min(value = 1, message = "평점은 1~5 사이여야 합니다.")
    @Max(value = 5, message = "평점은 1~5 사이여야 합니다.")
    @Schema(description = "평점 (1~5)", example = "5")
    private Integer rating;

    @NotBlank(message = "리뷰 내용은 필수입니다.")
    @Size(max = 1000, message = "리뷰 내용은 1000자를 초과할 수 없습니다.")
    @Schema(description = "리뷰 내용", example = "매우 만족스러운 상담이었습니다.")
    private String content;

    @Size(max = 5, message = "이미지는 최대 5개까지 업로드 가능합니다.")
    @Schema(description = "이미지 URL 목록 (S3에 업로드된 이미지)")
    private List<String> imageUrls;

    @Size(max = 5, message = "해시태그는 최대 5개까지 입력 가능합니다.")
    @Schema(description = "해시태그 목록", example = "[\"포마드\", \"댄디펌\"]")
    private List<String> hashtags;
}