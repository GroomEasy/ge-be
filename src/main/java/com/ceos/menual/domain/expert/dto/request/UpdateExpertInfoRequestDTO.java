package com.ceos.menual.domain.expert.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "전문가 소개서 정보 수정 요청")
public class UpdateExpertInfoRequestDTO {

	@Schema(description = "한 줄 소개(최대 100자). 빈 문자열 전달 시 삭제 처리.", example = "손상모 복구/레이어드 전문입니다.")
	@Size(max = 100, message = "한 줄 소개는 100자 이내여야 합니다.")
	private String introduction;

	@Schema(description = "인스타그램 링크(또는 프로필 링크). 빈 문자열 전달 시 삭제 처리.", example = "https://instagram.com/your_id")
	@Size(max = 255, message = "링크는 255자 이내여야 합니다.")
	private String profileLink;

	@Schema(description = "경력 정보. 빈 문자열 전달 시 삭제 처리.", example = "2021-2023 청담동 XX샵 근무\n전) OO샵 원장")
	@Size(max = 500, message = "경력 정보는 500자 이내여야 합니다.")
	private String careerInfo;

	@JsonIgnore
	@AssertTrue(message = "introduction, profileLink, careerInfo 중 최소 1개는 입력해야 합니다.")
	public boolean isAtLeastOneProvided() {
		return introduction != null || profileLink != null || careerInfo != null;
	}
}

