package com.ceos.menual.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointHistoryListResponseDTO {

    private Integer totalPoints;                    // 현재 보유 포인트
    private List<PointHistoryResponseDTO> history;  // 포인트 내역 리스트
}