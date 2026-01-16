package com.ceos.menual.domain.expert.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "대표 포트폴리오 해제 응답 DTO")
public class UnsetRepresentativePortfolioResponseDTO {

    @Schema(description = "메시지", example = "대표 포트폴리오가 해제되었습니다.")
    private String message;
}