package com.ceos.menual.domain.reservation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "예약 가능 시간 등록 요청")
@Getter
@Setter
public class CreateAvailableScheduleRequestDTO {

    @Schema(description = "예약 가능 날짜", example = "2026-01-28", required = true)
    @NotNull(message = "날짜는 필수입니다")
    private LocalDate availableDate;

    @Schema(
            description = "예약 가능 시간 목록",
            example = "[\"09:00\", \"09:30\", \"10:00\", \"10:30\", \"11:00\"]",
            required = true
    )
    @NotEmpty(message = "최소 1개 이상의 시간을 선택해야 합니다")
    private List<LocalTime> availableTimes;
}