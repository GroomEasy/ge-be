package com.ceos.menual.domain.reservation.dto.request;

import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.entity.enums.ConsultationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Schema(description = "임시 예약 생성 요청")
@Getter
@Setter
@NoArgsConstructor
public class CreateTempReservationRequestDTO {

    @Schema(description = "전문가 ID (User ID)", example = "3", required = true)
    @NotNull(message = "전문가 ID는 필수입니다")
    private Long expertId;

    @Schema(description = "상담 카테고리", example = "HAIR", required = true,
            allowableValues = {"HAIR", "FASHION", "SKIN", "MAKEUP"})
    @NotNull(message = "카테고리는 필수입니다")
    private Category category;

    @Schema(description = "상담 유형", example = "VIDEO", required = true,
            allowableValues = {"VIDEO", "MESSAGE"})
    @NotNull(message = "상담 유형은 필수입니다")
    private ConsultationType consultationType;

    @Schema(description = "예약 날짜 및 시간 (VIDEO 상담인 경우 필수)", example = "2026-01-28T11:30:00")
    @Future(message = "예약 시간은 미래여야 합니다")
    private LocalDateTime scheduledDateTime;

    @Schema(description = "상담 가격", example = "40000", required = true)
    @NotNull(message = "가격은 필수입니다")
    @Positive(message = "가격은 0보다 커야 합니다")
    private Integer price;
}