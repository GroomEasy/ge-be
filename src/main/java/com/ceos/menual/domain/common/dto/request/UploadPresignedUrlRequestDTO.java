package com.ceos.menual.domain.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "S3 업로드용 Presigned URL 발급 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UploadPresignedUrlRequestDTO {

	@Schema(description = "리소스 타입", example = "consultation")
	@NotBlank(message = "리소스 타입은 필수입니다. (예: consultation, review)")
	private String resourceType;

	@Schema(description = "리소스 ID (예약/상담 ID)", example = "123", required = true)
	@NotNull(message = "리소스 ID는 필수입니다.")
	private Long resourceId;

	@Schema(description = "이미지 타입", example = "hairstyle")
	@NotBlank(message = "이미지 타입은 필수입니다. (예: hairstyle, favorite, purpose)")
	private String imageType;

	@Schema(description = "파일명", example = "1.jpg")
	@NotBlank(message = "파일명은 필수입니다.")
	private String fileName;
}

