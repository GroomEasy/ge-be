package com.ceos.menual.domain.common.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ceos.menual.domain.common.dto.request.PresignedUrlRequestDTO;
import com.ceos.menual.domain.common.dto.response.PresignedUrlResponseDTO;
import com.ceos.menual.domain.common.service.S3PresignedUrlService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * S3 Presigned URL 공통 API Controller
 * consultation, review 등 여러 도메인에서 사용 가능
 */
@RestController
@RequestMapping("/api/presigned-urls")
@Validated
@RequiredArgsConstructor
@Tag(name = "S3 Presigned URL", description = "S3 파일 업로드/다운로드 관련 공통 API")
public class S3PresignedUrlController {

	private final S3PresignedUrlService s3PresignedUrlService;

	/**
	 * S3 업로드용 Presigned URL 발급
	 * 
	 * @param request 리소스 타입, ID와 파일명을 포함한 요청
	 * @return 업로드용 Presigned URL
	 * 
	 * 예시:
	 * - resourceType: "consultation", resourceId: 1, fileName: "hairstyle.jpg"
	 * - resourceType: "review", resourceId: 5, fileName: "1.jpg"
	 */
	@PostMapping("/upload")
	@Operation(
		summary = "업로드용 Presigned URL 발급",
		description = "S3에 파일을 업로드하기 위한 Presigned URL을 발급합니다. 유효시간은 15분입니다."
	)
	public ResponseEntity<PresignedUrlResponseDTO> getUploadPresignedUrl(
		@Valid @RequestBody PresignedUrlRequestDTO request) {

		String uploadUrl = s3PresignedUrlService.generateUploadPresignedUrl(
			request.getResourceType(),
			request.getResourceId(),
			request.getFileName()
		);

		String s3Key = s3PresignedUrlService.buildS3Key(
			request.getResourceType(),
			request.getResourceId(),
			request.getFileName());

		PresignedUrlResponseDTO response = PresignedUrlResponseDTO.builder()
			.s3Key(s3Key)
			.uploadUrl(uploadUrl)
			.expiresIn(900L) // 15분 = 900초
			.message("업로드용 URL이 발급되었습니다. 15분 내에 업로드해주세요.")
			.build();

		return ResponseEntity.ok(response);
	}

	/**
	 * S3 다운로드용 Presigned URL 발급
	 * 
	 * @param request 리소스 타입, ID와 파일명을 포함한 요청
	 * @return 다운로드용 Presigned URL
	 */
	@PostMapping("/download")
	@Operation(
		summary = "다운로드용 Presigned URL 발급",
		description = "S3에서 파일을 다운로드하기 위한 Presigned URL을 발급합니다. 유효시간은 1시간입니다."
	)
	public ResponseEntity<PresignedUrlResponseDTO> getDownloadPresignedUrl(
		@Valid @RequestBody PresignedUrlRequestDTO request) {

		String downloadUrl = s3PresignedUrlService.generateDownloadPresignedUrl(
			request.getResourceType(),
			request.getResourceId(),
			request.getFileName()
		);

		String s3Key = s3PresignedUrlService.buildS3Key(
			request.getResourceType(),
			request.getResourceId(),
			request.getFileName());

		PresignedUrlResponseDTO response = PresignedUrlResponseDTO.builder()
			.s3Key(s3Key)
			.downloadUrl(downloadUrl)
			.expiresIn(3600L) // 1시간 = 3600초
			.message("다운로드용 URL이 발급되었습니다. 1시간 내에 다운로드해주세요.")
			.build();

		return ResponseEntity.ok(response);
	}
}

