package com.ceos.menual.domain.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Presigned URL 응답 DTO (공통)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresignedUrlResponseDTO {

	private String s3Key;
	private String uploadUrl;
	private String downloadUrl;
	private Long expiresIn;
	private String message;
}

