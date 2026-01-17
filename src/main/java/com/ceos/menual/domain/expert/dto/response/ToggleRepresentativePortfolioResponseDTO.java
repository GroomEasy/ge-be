package com.ceos.menual.domain.expert.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ToggleRepresentativePortfolioResponseDTO {
    private Long portfolioId;
    private Boolean isRepresentative; // 토글 후 최종 상태 (true/false)
}