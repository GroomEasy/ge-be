package com.ceos.menual.domain.user.dto.request;

import com.ceos.menual.entity.enums.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "전문가 전환 요청 DTO")
public class ExpertConversionRequestDTO {

    @NotNull(message = "사용자 ID는 필수입니다.")
    @Schema(description = "전환할 사용자 ID", example = "1", required = true)
    private Long userId;

    @NotNull(message = "카테고리는 필수입니다.")
    @Schema(description = "전문가 카테고리", example = "HAIR", required = true)
    private Category category;

    @Schema(description = "전문 분야 리스트 (선택)", example = "[\"다운펌\", \"댄디펌\"]")
    private List<String> specialities;

    @Schema(description = "한줄소개 (선택)", example = "10년차 헤어 스타일 전문가입니다.")
    private String introduction;

    @Schema(description = "프로필 링크 (선택)", example = "https://menual.site/")
    private String profileLink;

    @Schema(description = "경력정보 (선택)", example = "준O헤어 근무\n...")
    private String careerInfo;
}
