package com.ceos.menual.domain.expert.dto.request;

import com.ceos.menual.entity.enums.ConsultationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "상담 유형 활성화/비활성화 요청 DTO")
public class ConsultationTypeUpdateRequestDTO {

    @NotEmpty(message = "상담 유형 목록은 비어있을 수 없습니다.")
    @Valid
    @Schema(description = "수정할 상담 유형 목록")
    private List<TypeItem> types;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "개별 상담 유형 항목")
    public static class TypeItem {

        @NotNull(message = "상담 유형은 필수입니다.")
        @Schema(description = "상담 유형 (VIDEO, MESSAGE)", example = "VIDEO", required = true)
        private ConsultationType consultationType;

        @NotNull(message = "활성화 여부는 필수입니다.")
        @Schema(description = "활성화 여부", example = "true", required = true)
        private Boolean isActive;
    }
}
