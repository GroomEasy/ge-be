package com.ceos.menual.domain.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReviewResponseDTO {

    private Long reviewId;
    private Long consultationId;
    private String message;

    public static CreateReviewResponseDTO of(Long reviewId, Long consultationId) {
        return CreateReviewResponseDTO.builder()
                .reviewId(reviewId)
                .consultationId(consultationId)
                .message("후기가 성공적으로 작성되었습니다.")
                .build();
    }
}