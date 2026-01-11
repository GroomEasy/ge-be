package com.ceos.menual.domain.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompletedReviewResponseDTO {

    private Long reviewId;

    // 상담 정보
    private LocalDateTime consultationDate;

    // 전문가 정보
    private String expertName;

    // 후기 내용
    private Integer rating;
    private String content;
    private List<String> imageUrls; // 후기 사진
}