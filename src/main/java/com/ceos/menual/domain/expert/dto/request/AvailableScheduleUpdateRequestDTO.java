package com.ceos.menual.domain.expert.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "예약 가능 시간 수정 요청 DTO")
public class AvailableScheduleUpdateRequestDTO {

    @NotNull(message = "날짜는 필수입니다.")
    @Schema(description = "예약 가능 날짜", example = "2026-01-28", required = true)
    private LocalDate availableDate;

    @NotEmpty(message = "최소 1개 이상의 시간을 선택해야 합니다.")
    @Schema(
            description = "예약 가능 시간 목록 (해당 날짜에 설정할 모든 시간)",
            example = "[\"09:00\", \"09:30\", \"10:00\", \"10:30\", \"11:00\"]",
            required = true
    )
    private List<LocalTime> availableTimes;
}
