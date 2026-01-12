package com.ceos.menual.domain.common.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ceos.menual.domain.common.dto.request.DownloadPresignedUrlRequestDTO;
import com.ceos.menual.domain.common.dto.request.UploadPresignedUrlRequestDTO;
import com.ceos.menual.domain.common.dto.response.PresignedUrlResponseDTO;
import com.ceos.menual.domain.common.service.S3PresignedUrlService;
import com.ceos.menual.domain.common.service.S3PresignedUrlService.GeneratePresignedUrlResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * S3 Presigned URL 공통 API Controller
 * consultation, review 등 여러 도메인에서 사용 가능
 */
@Slf4j
@RestController
@RequestMapping("/api/presigned-urls")
@Validated
@RequiredArgsConstructor
@Tag(name = "S3 Presigned URL", description = "S3 파일 업로드/다운로드 관련 공통 API")
public class S3PresignedUrlController {

	private final S3PresignedUrlService s3PresignedUrlService;

	/**
	 * S3 업로드용 Presigned URL 발급 (임시 저장)
	 * 
	 * @param request resourceType, imageType, fileName을 포함한 요청
	 * @return 업로드용 Presigned URL과 S3 Key
	 * 
	 * 저장 경로: tmp/{resourceType}/user-{userId}/{imageType}/{fileName}
	 * 결제 확인 후: final/{resourceType}/{resourceId}/{imageType}/{fileName}로 이동
	 * 
	 * resourceType 예시: consultation, review, portfolio 등
	 * imageType 예시:
	 *   - 헤어: hairstyle, front, left, right, favorite, difficulty
	 *   - 패션: front, left, right, favorite, purpose
	 */
	@PostMapping("/upload")
	@Operation(
		summary = "업로드용 Presigned URL 발급",
		description = "S3에 이미지를 임시로 업로드하기 위한 Presigned URL을 발급합니다. " +
					"resourceType(consultation, review 등)과 imageType(hairstyle, favorite 등)을 지정합니다. " +
					"유효시간은 15분입니다."
	)
	public ResponseEntity<PresignedUrlResponseDTO> getUploadPresignedUrl(
		@Valid @RequestBody UploadPresignedUrlRequestDTO request,
		@AuthenticationPrincipal Long userId) {

		log.info("Presigned URL 요청 - 사용자: {}, 리소스ID: {}, 이미지타입: {}", userId, request.getResourceId(), request.getImageType());

		GeneratePresignedUrlResponse response = s3PresignedUrlService.generateUploadPresignedUrl(
			request.getResourceType(),
			request.getResourceId(),
			request.getImageType(),
			request.getFileName()
		);

		return ResponseEntity.ok(PresignedUrlResponseDTO.builder()
			.s3Key(response.getS3Key())
			.uploadUrl(response.getUploadUrl())
			.expiresIn(response.getExpiresIn())
			.message("업로드용 URL이 발급되었습니다. 15분 내에 업로드해주세요.")
			.build());
	}

	/**
	 * S3 다운로드용 Presigned URL 발급
	 * 
	 * @param request 리소스 타입, 이미지 타입, ID와 파일명을 포함한 요청
	 * @return 다운로드용 Presigned URL
	 */
	@PostMapping("/download")
	@Operation(
		summary = "파일 다운로드용 Presigned URL 발급",
		description = "S3에서 파일을 다운로드하기 위한 Presigned URL을 발급합니다. 유효시간은 1시간입니다."
	)
	public ResponseEntity<PresignedUrlResponseDTO> getDownloadPresignedUrl(
		@Valid @RequestBody DownloadPresignedUrlRequestDTO request) {

		GeneratePresignedUrlResponse response = s3PresignedUrlService.generateDownloadPresignedUrl(
			request.getResourceType(),
			request.getImageType(),
			request.getResourceId(),
			request.getFileName()
		);

		return ResponseEntity.ok(PresignedUrlResponseDTO.builder()
			.s3Key(response.getS3Key())
			.downloadUrl(response.getDownloadUrl())
			.expiresIn(response.getExpiresIn())
			.message("다운로드용 URL이 발급되었습니다. 1시간 내에 다운로드해주세요.")
			.build());
	}
}

