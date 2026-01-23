package com.ceos.menual.domain.consultation.dto.response;

import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.entity.enums.ConsultationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Builder
public class SolutionListResponseDTO {

    private Long consultationId;
    private LocalDateTime date;
    private Long expertUserId;
    private String expertName;
    private Category category;
    private ConsultationType consultationType;

    /**
     * QueryDSL Constructor용 생성자
     */
    public SolutionListResponseDTO(
            Long consultationId,
            LocalDateTime date,
            Long expertUserId,
            String expertName,
            Category category,
            ConsultationType consultationType
    ) {
        this.consultationId = consultationId;
        this.date = date;
        this.expertUserId = expertUserId;
        this.expertName = expertName;
        this.category = category;
        this.consultationType = consultationType;
    }
}