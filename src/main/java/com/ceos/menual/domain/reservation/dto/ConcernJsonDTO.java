package com.ceos.menual.domain.reservation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "고민지 JSON 구조")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcernJsonDTO {

    @Schema(description = "S3 이미지 키 목록 (tmp 또는 final 폴더의 이미지 경로)", example = "[\"tmp/hairstyle.jpg\", \"tmp/front.jpg\", \"tmp/favorite/1.jpg\", \"tmp/favorite/2.jpg\", \"tmp/purpose/1.jpg\"]")
    @JsonProperty("imageKeys")
    private List<String> imageKeys;

    @Schema(description = "추구미 (원하는 스타일 설명)", example = "자연스럽고 볼륨감 있는 스타일을 원합니다")
    @JsonProperty("desiredStyle")
    private String desiredStyle;

    @Schema(description = "상담 목적 (상담 목표 및 고민사항)", example = "현재 머리가 손상되어 있고 매직이 필요합니다")
    @JsonProperty("consultationPurpose")
    private String consultationPurpose;
}



