package com.ceos.menual.domain.expert.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "대표 포트폴리오 지정 응답 DTO")
public class SetRepresentativePortfolioResponseDTO {

    @Schema(description = "대표로 지정된 포트폴리오 ID", example = "5")
    private Long portfolioId;

    @Schema(description = "메시지", example = "대표 포트폴리오가 변경되었습니다.")
    private String message;
}