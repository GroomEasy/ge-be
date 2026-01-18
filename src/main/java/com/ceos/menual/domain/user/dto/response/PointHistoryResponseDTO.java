package com.ceos.menual.domain.user.dto.response;

import com.ceos.menual.entity.PointHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointHistoryResponseDTO {

    private LocalDateTime date;         // 날짜
    private Integer point_amount;             // 포인트 금액 (양수: 적립, 음수: 차감)
    private String description;         // 설명

    public static PointHistoryResponseDTO from(PointHistory pointHistory) {
        return PointHistoryResponseDTO.builder()
                .date(pointHistory.getCreatedAt())
                .point_amount(pointHistory.getPoint())
                .description(pointHistory.getDescription())
                .build();
    }
}