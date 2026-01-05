package com.ceos.menual.domain.common.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Presigned URL 발급 요청 DTO (공통)
 * 예시:
 * - resourceType: "consultation", resourceId: 1, fileName: "hairstyle.jpg"
 * - resourceType: "review", resourceId: 5, fileName: "1.jpg"
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlRequestDTO {

	@NotBlank(message = "리소스 타입은 필수입니다.")
	private String resourceType; // consultation, review 등

	@NotNull(message = "리소스 ID는 필수입니다.")
	private Long resourceId;

	@NotBlank(message = "파일명은 필수입니다.")
	private String fileName;
}

