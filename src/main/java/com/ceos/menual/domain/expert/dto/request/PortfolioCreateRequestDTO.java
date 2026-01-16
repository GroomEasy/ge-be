package com.ceos.menual.domain.expert.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "포트폴리오 등록 요청 DTO")
public class PortfolioCreateRequestDTO {

    @NotBlank(message = "시술명은 필수입니다.")
    @Size(max = 100, message = "시술명은 100자 이내여야 합니다.")
    @Schema(description = "시술명/제목", example = "손상모 복구 레이어드 컷", required = true)
    private String title;

    @NotBlank(message = "고민은 필수입니다.")
    @Size(max = 500, message = "고민은 500자 이내여야 합니다.")
    @Schema(description = "고객의 고민", example = "잦은 탈색으로 모발 끝이 갈라지고 부스스함", required = true)
    private String concern;

    @NotBlank(message = "솔루션은 필수입니다.")
    @Size(max = 500, message = "솔루션은 500자 이내여야 합니다.")
    @Schema(description = "해결 솔루션", example = "단백질 케어와 함께 레이어드 컷으로 손상 부위 제거", required = true)
    private String solution;

    @Schema(description = "시술 전 이미지 URL", example = "https://image.com/before.jpg")
    private String beforeImage;

    @Schema(description = "시술 후 이미지 URL", example = "https://image.com/after.jpg")
    private String afterImage;

    @Size(max = 10, message = "해시태그는 최대 10개까지 가능합니다.")
    @Schema(description = "해시태그 이름 리스트 (최대 10개)", example = "[\"레이어드컷\", \"복구펌\", \"가을헤어\"]")
    private List<String> hashtags;
}