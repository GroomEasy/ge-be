package com.ceos.menual.domain.reservation.dto.request;

import com.ceos.menual.domain.reservation.dto.HairConcernDTO;
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

@Schema(description = "헤어 상담 고민지(concernJson) 업데이트 요청")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHairConcernRequestDTO {

    @Schema(description = "헤어 상담 고민지", required = true)
    @NotNull(message = "헤어 상담 고민지는 필수입니다")
    @Valid
    @JsonProperty("hair")
    private HairConcernDTO hair;
}
