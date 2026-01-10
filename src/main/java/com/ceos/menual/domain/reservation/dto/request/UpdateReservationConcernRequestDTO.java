package com.ceos.menual.domain.reservation.dto.request;

import com.ceos.menual.domain.reservation.dto.FashionConcernDTO;
import com.ceos.menual.domain.reservation.dto.HairConcernDTO;
import com.ceos.menual.entity.enums.Category;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @Schema(description = "상담 카테고리", example = "FASHION", allowableValues = {"FASHION", "HAIR"}, required = true)
    @NotNull(message = "상담 카테고리는 필수입니다")
    @JsonProperty("type")
    private Category category;

    @Schema(description = "패션 상담 고민지 (type이 FASHION일 때 필수)")
    @JsonProperty("fashion")
    private FashionConcernDTO fashion;

    @Schema(description = "헤어 상담 고민지 (type이 HAIR일 때 필수)")
    @JsonProperty("hair")
    private HairConcernDTO hair;

    @Schema(description = "S3 이미지 키 목록", example = "[\"tmp/consultation/user-123/purpose/1.jpg\"]", required = true)
    @NotEmpty(message = "최소 1개 이상의 이미지 키를 포함해야 합니다")
    private List<String> imageKeys;
}




