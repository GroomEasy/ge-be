package com.ceos.menual.domain.common.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * S3 Presigned URL 발급 서비스 (공통)
 * consultation, review 등 여러 도메인에서 사용 가능
 */
@Service
public class S3PresignedUrlService {

	@Value("${aws.s3.bucket-name}")
	private String bucketName;

	private final S3Presigner s3Presigner;

	public S3PresignedUrlService(S3Presigner s3Presigner) {
		this.s3Presigner = s3Presigner;
	}

	/**
	 * Presigned URL 발급 (업로드용)
	 * @param resourceType 리소스 타입 (consultation, review 등)
	 * @param resourceId 리소스 ID
	 * @param fileName 파일명 (예: hairstyle.jpg, front.jpg, favorite/1.jpg)
	 * @return Presigned URL
	 */
	public String generateUploadPresignedUrl(String resourceType, Long resourceId, String fileName) {
		String s3Key = buildS3Key(resourceType, resourceId, fileName, true);

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(s3Key)
			.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(Duration.ofMinutes(15))
			.putObjectRequest(putObjectRequest)
			.build();

		return s3Presigner.presignPutObject(presignRequest).url().toString();
	}

	/**
	 * Presigned URL 발급 (다운로드용)
	 * @param resourceType 리소스 타입 (consultation, review 등)
	 * @param resourceId 리소스 ID
	 * @param fileName 파일명
	 * @return Presigned URL
	 */
	public String generateDownloadPresignedUrl(String resourceType, Long resourceId, String fileName) {
		String s3Key = buildS3Key(resourceType, resourceId, fileName, false);

		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
			.bucket(bucketName)
			.key(s3Key)
			.build();

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
			.signatureDuration(Duration.ofHours(1))
			.getObjectRequest(getObjectRequest)
			.build();

		return s3Presigner.presignGetObject(presignRequest).url().toString();
	}

	/**
	 * S3 Key 생성
	 * @param resourceType 리소스 타입 (consultation, review 등)
	 * @param resourceId 리소스 ID
	 * @param fileName 파일명
	 * @param isTemporary 임시 경로 여부 (true: tmp/ 접두사 추가)
	 * @return S3 Key (예: tmp/consultation/123/hairstyle.jpg)
	 */
	public String buildS3Key(String resourceType, Long resourceId, String fileName, boolean isTemporary) {
		String basePath = isTemporary ? "tmp/" : "";
		return String.format("%s%s/%d/%s", basePath, resourceType, resourceId, fileName);
	}
}

