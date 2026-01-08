package com.ceos.menual.domain.reservation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Schema(description = "고민지(concernJson) 업데이트 요청")
@Getter
@Setter
@NoArgsConstructor
public class UpdateReservationConcernRequestDTO {

    @Schema(description = "S3 이미지 키 목록 (임시 저장 경로: tmp/{resourceType}/user-{userId}/{imageType}/{fileName})", example = "[\"tmp/consultation/user-123/purpose/1.jpg\", \"tmp/consultation/user-123/favorite/2.jpg\", \"tmp/consultation/user-123/favorite/3.jpg\"]")
    @NotEmpty(message = "최소 1개 이상의 이미지 키를 포함해야 합니다")
    private List<String> imageKeys;

    @Schema(description = "추구미 (원하는 스타일 설명)", example = "자연스럽고 볼륨감 있는 스타일을 원합니다", required = true)
    @NotNull(message = "추구미는 필수입니다")
    private String desiredStyle;

    @Schema(description = "상담 목적 (상담 목표 및 고민사항)", example = "현재 머리가 손상되어 있고 매직이 필요합니다", required = true)
    @NotNull(message = "상담 목적은 필수입니다")
    private String consultationPurpose;
}



