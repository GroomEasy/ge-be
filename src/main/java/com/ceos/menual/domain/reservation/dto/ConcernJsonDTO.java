package com.ceos.menual.domain.reservation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "고민지 JSON 구조 - 패션/헤어 상담 통합")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConcernJsonDTO {

    @Schema(description = "상담 타입", example = "FASHION", allowableValues = {"FASHION", "HAIR"})
    @JsonProperty("type")
    private String type;

    @Schema(description = "패션 상담 고민지")
    @JsonProperty("fashion")
    private FashionConcernDTO fashion;

    @Schema(description = "헤어 상담 고민지")
    @JsonProperty("hair")
    private HairConcernDTO hair;

    @Schema(description = "추구미 (원하는 스타일 설명) - 하위 호환성", example = "자연스럽고 볼륨감 있는 스타일을 원합니다")
    @JsonProperty("desiredStyle")
    private String desiredStyle;

    @Schema(description = "상담 목적 (상담 목표 및 고민사항) - 하위 호환성", example = "현재 머리가 손상되어 있고 매직이 필요합니다")
    @JsonProperty("consultationPurpose")
    private String consultationPurpose;
}



