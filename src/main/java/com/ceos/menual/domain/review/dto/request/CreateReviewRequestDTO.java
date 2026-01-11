package com.ceos.menual.domain.review.dto.request;

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
    private Long consultationId;

    @NotNull(message = "평점은 필수입니다.")
    @Min(value = 1, message = "평점은 1 이상이어야 합니다.")
    @Max(value = 5, message = "평점은 5 이하여야 합니다.")
    private Integer rating;

    @NotBlank(message = "후기 내용은 필수입니다.")
    @Size(min = 30, max = 1000, message = "후기 내용은 30자 이상 1000자 이하여야 합니다.")
    private String content;

    // 후기 이미지 URL 목록
    @Size(max = 5, message = "이미지는 최대 5개까지 등록 가능합니다.")
    private List<String> imageUrls;
}