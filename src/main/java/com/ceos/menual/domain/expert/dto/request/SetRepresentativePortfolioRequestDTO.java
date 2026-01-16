package com.ceos.menual.domain.expert.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대표 포트폴리오 지정 요청 DTO")
public class SetRepresentativePortfolioRequestDTO {

    @NotNull(message = "포트폴리오 ID는 필수입니다.")
    @Schema(description = "대표로 지정할 포트폴리오 ID", example = "5", required = true)
    private Long portfolioId;
}