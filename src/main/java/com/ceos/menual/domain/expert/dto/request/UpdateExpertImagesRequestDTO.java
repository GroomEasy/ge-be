package com.ceos.menual.domain.expert.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "전문가 프로필/배경 이미지 수정 요청 (S3 final key 입력)")
public class UpdateExpertImagesRequestDTO {

	@Schema(
		description = "프로필 이미지 S3 final key (선택)",
		example = "final/expert/123/profile/550e8400-e29b-41d4-a716-446655440000.jpg"
	)
	private String profileImageKey;

	@Schema(
		description = "배경 이미지 S3 final key (선택)",
		example = "final/expert/123/background/550e8400-e29b-41d4-a716-446655440000.jpg"
	)
	private String backgroundImageKey;

	@JsonIgnore
	@AssertTrue(message = "profileImageKey 또는 backgroundImageKey 중 하나는 필수입니다.")
	public boolean isAtLeastOneProvided() {
		return (profileImageKey != null && !profileImageKey.trim().isEmpty())
			|| (backgroundImageKey != null && !backgroundImageKey.trim().isEmpty());
	}
}

