package com.ceos.menual.domain.consultation.dto.response;

import com.ceos.menual.entity.ConsultationSchedule;
import com.ceos.menual.entity.enums.ConsultationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "상담 스케줄 정보")
@Getter
@Builder
public class ConsultationScheduleResponseDTO {

    @Schema(description = "상담 유형", example = "VIDEO")
    private ConsultationType consultationType;

    @Schema(description = "가격", example = "50000")
    private Integer price;

    @Schema(description = "활성화 여부", example = "true")
    private Boolean isActive;

    public static ConsultationScheduleResponseDTO from(ConsultationSchedule schedule) {
        return ConsultationScheduleResponseDTO.builder()
                .consultationType(schedule.getConsultationType())
                .price(schedule.getPrice())
                .isActive(schedule.getIsActive())
                .build();
    }
}
