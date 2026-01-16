package com.ceos.menual.domain.expert.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;



@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertPortfolioResponseDTO {

    @Schema(description = "포트폴리오 ID", example = "1")
    private Long id;

    @Schema(description = "시술명/제목", example = "손상모 복구 레이어드 컷")
    private String title;

    @Schema(description = "고객의 고민", example = "잦은 탈색으로 모발 끝이 갈라지고 부스스함")
    private String concern;

    @Schema(description = "해결 솔루션", example = "단백질 케어와 함께 레이어드 컷으로 손상 부위 제거")
    private String solution;

    @Schema(description = "대표 이미지 여부", example = "false")
    private Boolean isRepresentative;

    @Schema(description = "시술 전 이미지 URL", example = "https://image.com/before.jpg")
    private String beforeImage;

    @Schema(description = "시술 후 이미지 URL", example = "https://image.com/after.jpg")
    private String afterImage;

    @Schema(description = "해시태그 리스트", example = "[\"레이어드컷\", \"복구펌\", \"가을헤어\"]")
    private List<String> hashtags;
}