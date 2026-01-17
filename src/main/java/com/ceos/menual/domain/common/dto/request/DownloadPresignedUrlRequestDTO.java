package com.ceos.menual.domain.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "S3 다운로드용 Presigned URL 발급 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DownloadPresignedUrlRequestDTO {

	@Schema(description = "리소스 타입", example = "consultation")
	@NotBlank(message = "리소스 타입은 필수입니다.")
	private String resourceType;

	@Schema(description = "이미지 타입 (consultation: hairstyle/front/left/right/favorite/difficulty/purpose/solution, solution 다운로드는 생략 가능(자동 solution), review: 사용 안 함, portfolio: before/after)", example = "favorite")
	private String imageType;

	@Schema(description = "리소스 ID", example = "123")
	@NotNull(message = "리소스 ID는 필수입니다.")
	private Long resourceId;

	@Schema(description = "파일명", example = "2.jpg")
	@NotBlank(message = "파일명은 필수입니다.")
	private String fileName;
}

