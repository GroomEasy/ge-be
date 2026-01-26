package com.ceos.menual.domain.expert.dto.request;

import com.ceos.menual.entity.enums.ConsultationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "상담 스케줄 수정 요청 DTO")
public class ConsultationScheduleUpdateRequestDTO {

    @NotEmpty(message = "스케줄 목록은 비어있을 수 없습니다.")
    @Valid
    @Schema(description = "수정할 상담 스케줄 목록")
    private List<ScheduleItem> schedules;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "개별 상담 스케줄 항목")
    public static class ScheduleItem {

        @NotNull(message = "상담 유형은 필수입니다.")
        @Schema(description = "상담 유형 (VIDEO, MESSAGE)", example = "VIDEO", required = true)
        private ConsultationType consultationType;

        @NotNull(message = "가격은 필수입니다.")
        @Schema(description = "상담 가격", example = "50000", required = true)
        private Integer price;

        @NotNull(message = "활성화 여부는 필수입니다.")
        @Schema(description = "활성화 여부", example = "true", required = true)
        private Boolean isActive;
    }
}
