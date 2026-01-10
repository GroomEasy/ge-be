package com.ceos.menual.domain.reservation.dto.request;

import com.ceos.menual.domain.reservation.dto.FashionConcernDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Schema(description = "패션 상담 고민지(concernJson) 업데이트 요청")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFashionConcernRequestDTO {

    @Schema(description = "패션 상담 고민지", required = true)
    @NotNull(message = "패션 상담 고민지는 필수입니다")
    @Valid
    @JsonProperty("fashion")
    private FashionConcernDTO fashion;

    @Schema(description = "S3 이미지 키 목록", example = "[\"tmp/consultation/user-123/purpose/1.jpg\"]", required = true)
    @NotEmpty(message = "최소 1개 이상의 이미지 키를 포함해야 합니다")
    private List<String> imageKeys;
}
