package com.ceos.menual.domain.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointApplicationResponseDTO {

    private Integer originalPrice;      // 정가
    private Integer pointsUsed;         // 사용한 포인트
    private Integer finalPrice;         // 최종 결제 금액 (정가 - 포인트)
    private Integer remainingPoints;    // 남은 사용 가능 포인트
}