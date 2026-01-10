package com.ceos.menual.domain.reservation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "선호 가격대 범위")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutfitPriceRangeDTO {

    @Schema(description = "최소 가격 (원)", example = "0", required = true)
    @NotNull(message = "최소 가격은 필수입니다")
    @Min(value = 0, message = "최소 가격은 0 이상이어야 합니다")
    @JsonProperty("minPrice")
    private Integer minPrice;

    @Schema(description = "최대 가격 (원)", example = "400000", required = true)
    @NotNull(message = "최대 가격은 필수입니다")
    @Min(value = 0, message = "최대 가격은 0 이상이어야 합니다")
    @JsonProperty("maxPrice")
    private Integer maxPrice;

    @AssertTrue(message = "최소 가격은 최대 가격보다 작거나 같아야 합니다")
    private boolean isPriceRangeValid() {
        // minPrice나 maxPrice가 null이면 다른 검증에서 처리되므로 여기서는 true 반환
        if (minPrice == null || maxPrice == null) {
            return true;
        }
        return minPrice <= maxPrice;
    }
}
