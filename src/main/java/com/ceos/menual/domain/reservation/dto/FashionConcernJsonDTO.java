package com.ceos.menual.domain.reservation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "패션 상담 고민지 JSON 저장 구조")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FashionConcernJsonDTO {

    @Schema(description = "상담 타입", example = "FASHION")
    @JsonProperty("type")
    private String type;

    @Schema(description = "패션 상담 고민지")
    @JsonProperty("fashion")
    private FashionConcernDTO fashion;

    @Schema(description = "S3 이미지 키 목록")
    @JsonProperty("imageKeys")
    private List<String> imageKeys;
}
