package com.ceos.menual.domain.user.dto.response;

import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.entity.enums.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "전문가 전환 응답 DTO")
public class ExpertConversionResponseDTO {

    @Schema(description = "전문가 프로필 ID")
    private Long expertProfileId;

    @Schema(description = "사용자 타입", example = "EXPERT")
    private UserType userType;

    @Schema(description = "카테고리", example = "PSYCHOLOGY")
    private Category category;

    @Schema(description = "닉네임")
    private String nickname;
}