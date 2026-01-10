package com.ceos.menual.domain.reservation.dto;

import com.ceos.menual.entity.enums.CoveringPart;
import com.ceos.menual.entity.enums.FaceAdvantage;
import com.ceos.menual.entity.enums.PursuedImage;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "헤어 상담 고민지")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HairConcernDTO {

    @Schema(description = "얼굴 장점 선택지", example = "[\"눈\", \"얼굴형\"]", required = true)
    @NotEmpty(message = "얼굴 장점은 최소 1개 이상 선택해야 합니다")
    @JsonProperty("faceAdvantages")
    private List<FaceAdvantage> faceAdvantages;

    @Schema(description = "얼굴 장점 기타 텍스트", example = "얼굴이 길어요")
    @JsonProperty("faceAdvantagesEtcText")
    private String faceAdvantagesEtcText;

    @Schema(description = "커버하고 싶은 부분 선택지", example = "[\"눈\", \"눈썹\"]", required = true)
    @NotEmpty(message = "커버하고 싶은 부분은 최소 1개 이상 선택해야 합니다")
    @JsonProperty("coveringParts")
    private List<CoveringPart> coveringParts;

    @Schema(description = "커버하고 싶은 부분 기타 텍스트", example = "넓은 이마")
    @JsonProperty("coveringPartsEtcText")
    private String coveringPartsEtcText;

    @Schema(description = "추구하는 이미지 선택지", example = "[\"자연스러움\", \"신뢰를 주는\"]", required = true)
    @NotEmpty(message = "추구하는 이미지는 최소 1개 이상 선택해야 합니다")
    @JsonProperty("pursuedImages")
    private List<PursuedImage> pursuedImages;

    @Schema(description = "스타일링 시 느낀 어려움이나 궁금증", example = "매일 아침 스타일링이 힘들어요", required = true)
    @NotNull(message = "스타일링 어려움은 필수입니다")
    @JsonProperty("stylingDifficulty")
    private String stylingDifficulty;

    // 기존 필드 (하위 호환성)
    @Schema(description = "S3 이미지 키 목록", example = "[\"tmp/consultation/user-123/hairstyle/1.jpg\"]")
    @JsonProperty("imageKeys")
    private List<String> imageKeys;

    @Schema(description = "추구미 (원하는 스타일 설명)", example = "자연스럽고 볼륨감 있는 스타일")
    @JsonProperty("desiredStyle")
    private String desiredStyle;

    @Schema(description = "상담 목적", example = "손상된 머리 케어 및 스타일 추천")
    @JsonProperty("consultationPurpose")
    private String consultationPurpose;
}
