package com.ceos.menual.domain.expert.dto.request;

import com.ceos.menual.entity.enums.ConsultationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "상담 가격 수정 요청 DTO")
public class ConsultationPriceUpdateRequestDTO {

    @NotEmpty(message = "가격 목록은 비어있을 수 없습니다.")
    @Valid
    @Schema(description = "수정할 가격 목록")
    private List<PriceItem> prices;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "개별 가격 항목")
    public static class PriceItem {

        @NotNull(message = "상담 유형은 필수입니다.")
        @Schema(description = "상담 유형 (VIDEO, MESSAGE)", example = "VIDEO", required = true)
        private ConsultationType consultationType;

        @NotNull(message = "가격은 필수입니다.")
        @Schema(description = "상담 가격", example = "50000", required = true)
        private Integer price;
    }
}
