package com.ceos.menual.domain.review.dto.response;

import com.ceos.menual.entity.enums.Category;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Builder
public class AvailableReviewResponseDTO {

    private Long consultationId;

    // 전문가 정보
    private String expertName;
    private String expertProfileImage;
    private Category category;

    // 상담 정보
    private LocalDateTime consultationDate;
    private String description;

    /**
     * QueryDSL Constructor용 생성자
     */
    public AvailableReviewResponseDTO(
            Long consultationId,
            String expertName,
            String expertProfileImage,
            Category category,
            LocalDateTime consultationDate,
            String description
    ) {
        this.consultationId = consultationId;
        this.expertName = expertName;
        this.expertProfileImage = expertProfileImage;
        this.category = category;
        this.consultationDate = consultationDate;
        this.description = description;
    }
}