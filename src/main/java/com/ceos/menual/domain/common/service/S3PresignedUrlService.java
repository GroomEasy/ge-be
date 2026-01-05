package com.ceos.menual.domain.common.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
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

	@Value("${aws.s3.region}")
	private String region;

	@Value("${aws.s3.access-key}")
	private String accessKey;

	@Value("${aws.s3.secret-key}")
	private String secretKey;

	/**
	 * Presigned URL 발급 (업로드용)
	 * @param resourceType 리소스 타입 (consultation, review 등)
	 * @param resourceId 리소스 ID
	 * @param fileName 파일명 (예: hairstyle.jpg, front.jpg, favorite/1.jpg)
	 * @return Presigned URL
	 */
	public String generateUploadPresignedUrl(String resourceType, Long resourceId, String fileName) {
		String s3Key = buildS3Key(resourceType, resourceId, fileName, true);

		try (S3Presigner presigner = S3Presigner.builder()
			.region(Region.of(region))
			.credentialsProvider(StaticCredentialsProvider.create(
				AwsBasicCredentials.create(accessKey, secretKey)
			))
			.build()) {

			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(bucketName)
				.key(s3Key)
				.build();

			PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
				.signatureDuration(Duration.ofMinutes(15))
				.putObjectRequest(putObjectRequest)
				.build();

			return presigner.presignPutObject(presignRequest).url().toString();
		}
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

		try (S3Presigner presigner = S3Presigner.builder()
			.region(Region.of(region))
			.credentialsProvider(StaticCredentialsProvider.create(
				AwsBasicCredentials.create(accessKey, secretKey)
			))
			.build()) {

			GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(bucketName)
				.key(s3Key)
				.build();

			GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
				.signatureDuration(Duration.ofHours(1))
				.getObjectRequest(getObjectRequest)
				.build();

			return presigner.presignGetObject(presignRequest).url().toString();
		}
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

