package com.ceos.menual.domain.reservation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "월별 예약 가능 날짜 응답")
@Getter
@Builder
public class AvailableDatesResponseDTO {

    @Schema(description = "연도", example = "2025")
    private Integer year;

    @Schema(description = "월", example = "10")
    private Integer month;

    @Schema(description = "예약 가능한 날짜 목록", example = "[\"2025-10-28\", \"2025-10-29\", \"2025-10-30\"]")
    private List<LocalDate> availableDates;
}