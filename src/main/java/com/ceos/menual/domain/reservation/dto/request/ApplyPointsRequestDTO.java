package com.ceos.menual.domain.reservation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyPointsRequestDTO {

    @NotNull(message = "사용할 포인트를 입력해주세요.")
    @Min(value = 0, message = "포인트는 0 이상이어야 합니다.")
    private Integer pointsToUse;
}