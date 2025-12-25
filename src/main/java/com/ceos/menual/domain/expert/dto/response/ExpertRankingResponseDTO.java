package com.ceos.menual.domain.expert.dto.response;

import com.ceos.menual.entity.enums.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "전문가 TOP3")
public class ExpertRankingResponseDTO {

	@Schema(description = "전문가 이름", example = "김철수")
	private String name;

	@Schema(description = "전문가 카테고리명", example = "헤어")
	private Category category;

	@Schema(description = "프로필 이미지", example = "http://image.png")
	private String profileImage;

	@Schema(description = "한 줄 소개", example = "안녕하세요! 헤어 전문가 김철수입니다.")
	private String introduction;
}
