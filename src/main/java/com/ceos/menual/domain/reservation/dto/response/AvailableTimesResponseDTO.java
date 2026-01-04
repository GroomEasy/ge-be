package com.ceos.menual.domain.reservation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "일별 예약 가능 시간 응답")
@Getter
@Builder
public class AvailableTimesResponseDTO {

    @Schema(description = "날짜", example = "2025-10-28")
    private LocalDate date;

    @Schema(description = "예약 가능한 시간대 목록", example = "[\"11:00\", \"11:30\", \"13:00\"]")
    private List<LocalTime> availableTimes;
}