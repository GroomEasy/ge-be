package com.ceos.menual.domain.reservation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Schema(description = "패션 상담 이미지 맵")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FashionImageListDTO {

    @Schema(description = "정면전신 이미지", example = "[\"tmp/consultation/user-123/front/1.jpg\"]", required = true)
    @NotEmpty(message = "정면전신 사진은 1개 필수입니다")
    @Size(min = 1, max = 1, message = "정면전신 사진은 정확히 1개여야 합니다")
    @JsonProperty("frontFullBody")
    private List<String> frontFullBody;

    @Schema(description = "왼쪽전신 이미지", example = "[\"tmp/consultation/user-123/left/1.jpg\"]", required = true)
    @NotEmpty(message = "왼쪽전신 사진은 1개 필수입니다")
    @Size(min = 1, max = 1, message = "왼쪽전신 사진은 정확히 1개여야 합니다")
    @JsonProperty("leftFullBody")
    private List<String> leftFullBody;

    @Schema(description = "오른쪽전신 이미지", example = "[\"tmp/consultation/user-123/right/1.jpg\"]", required = true)
    @NotEmpty(message = "오른쪽전신 사진은 1개 필수입니다")
    @Size(min = 1, max = 1, message = "오른쪽전신 사진은 정확히 1개여야 합니다")
    @JsonProperty("rightFullBody")
    private List<String> rightFullBody;

    @Schema(description = "가장 좋아하는 착장 이미지 (2~5개)", example = "[\"tmp/consultation/user-123/favorite/1.jpg\", \"tmp/consultation/user-123/favorite/2.jpg\"]", required = true)
    @NotEmpty(message = "가장 좋아하는 사진은 최소 2개 필수입니다")
    @Size(min = 2, max = 5, message = "가장 좋아하는 사진은 2~5개여야 합니다")
    @JsonProperty("favoriteOutfit")
    private List<String> favoriteOutfit;

    @Schema(description = "전문가 상담 목적 이미지 (0~3개)", example = "[\"tmp/consultation/user-123/consultation/1.jpg\"]")
    @Size(min = 0, max = 3, message = "전문가 상담 목적 사진은 0~3개여야 합니다")
    @JsonProperty("consultationPurpose")
    private List<String> consultationPurpose;
}
