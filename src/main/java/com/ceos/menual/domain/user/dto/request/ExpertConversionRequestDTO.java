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
@Schema(
        description = "전문가 전환 요청 DTO",
        example = """
        {
          "category": "HAIR",
          "specialities": [],
          "introduction": null,
          "profileLink": null,
          "careerInfo": null
        }
        """
)
public class ExpertConversionRequestDTO {

    @NotNull(message = "카테고리는 필수입니다.")
    @Schema(description = "전문가 카테고리", example = "HAIR", requiredMode = Schema.RequiredMode.REQUIRED)
    private Category category;

    @Schema(description = "전문 분야 리스트 (선택)", nullable = true)
    private List<String> specialities;

    @Schema(description = "한줄소개 (선택)", nullable = true)
    private String introduction;

    @Schema(description = "프로필 링크 (선택)", nullable = true)
    private String profileLink;

    @Schema(description = "경력정보 (선택)", nullable = true)
    private String careerInfo;
}
