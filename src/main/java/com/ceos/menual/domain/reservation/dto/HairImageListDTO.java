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

@Schema(description = "헤어 상담 이미지 맵")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HairImageListDTO {

    @Schema(description = "헤어스타일 이미지", example = "[\"tmp/consultation/user-123/hairstyle/1.jpg\"]", required = true)
    @NotEmpty(message = "헤어스타일 사진은 1개 필수입니다")
    @Size(min = 1, max = 1, message = "헤어스타일 사진은 정확히 1개여야 합니다")
    @JsonProperty("hairstyle")
    private List<String> hairstyle;

    @Schema(description = "정면 이미지", example = "[\"tmp/consultation/user-123/front/1.jpg\"]", required = true)
    @NotEmpty(message = "정면 사진은 1개 필수입니다")
    @Size(min = 1, max = 1, message = "정면 사진은 정확히 1개여야 합니다")
    @JsonProperty("front")
    private List<String> front;

    @Schema(description = "왼쪽 측면 이미지", example = "[\"tmp/consultation/user-123/left/1.jpg\"]", required = true)
    @NotEmpty(message = "왼쪽 측면 사진은 1개 필수입니다")
    @Size(min = 1, max = 1, message = "왼쪽 측면 사진은 정확히 1개여야 합니다")
    @JsonProperty("leftSide")
    private List<String> leftSide;

    @Schema(description = "오른쪽 측면 이미지", example = "[\"tmp/consultation/user-123/right/1.jpg\"]", required = true)
    @NotEmpty(message = "오른쪽 측면 사진은 1개 필수입니다")
    @Size(min = 1, max = 1, message = "오른쪽 측면 사진은 정확히 1개여야 합니다")
    @JsonProperty("rightSide")
    private List<String> rightSide;

    @Schema(description = "가장 마음에 드는 사진 (0~1개)", example = "[\"tmp/consultation/user-123/favorite/1.jpg\"]")
    @Size(min = 0, max = 1, message = "가장 마음에 드는 사진은 0~1개여야 합니다")
    @JsonProperty("favoriteStyle")
    private List<String> favoriteStyle;

    @Schema(description = "스타일링 어려움 이미지 (0~3개)", example = "[\"tmp/consultation/user-123/difficulty/1.jpg\"]")
    @Size(min = 0, max = 3, message = "스타일링 어려움 사진은 0~3개여야 합니다")
    @JsonProperty("stylingDifficulty")
    private List<String> stylingDifficulty;
}
