package com.ceos.menual.domain.common.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ceos.menual.domain.common.entity.S3CleanupTask;
import com.ceos.menual.domain.common.entity.S3CleanupTask.CleanupStatus;
import com.ceos.menual.domain.common.repository.S3CleanupTaskRepository;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * S3 Presigned URL 발급 서비스 (공통)
 * consultation, review 등 여러 도메인에서 사용 가능
 * 
 * 임시 파일 저장 경로: tmp/{resourceType}/reservation-{resourceId}/{imageType}/{fileName}
 * 최종 파일 저장 경로: final/{resourceType}/{resourceId}/{imageType}/{fileName}
 */
@Slf4j
@Service
public class S3PresignedUrlService {

	@Value("${aws.s3.bucket-name}")
	private String bucketName;

	@Value("${aws.s3.cleanup.max-retries:3}")
	private Integer maxRetries;

	@Value("${aws.s3.cleanup.retry-interval-ms:1000}")
	private Long retryIntervalMs;

	@Value("${aws.s3.region}")
	private String awsRegion;

	private final S3Presigner s3Presigner;
	private final S3Client s3Client;
	private final S3CleanupTaskRepository s3CleanupTaskRepository;

	public S3PresignedUrlService(S3Presigner s3Presigner, S3Client s3Client, 
			S3CleanupTaskRepository s3CleanupTaskRepository) {
		this.s3Presigner = s3Presigner;
		this.s3Client = s3Client;
		this.s3CleanupTaskRepository = s3CleanupTaskRepository;
	}

	/**
	 * Presigned URL 발급 (업로드용 - 임시 저장)
	 * 
	 * 경로 구조:
	 * - consultation(고민지 이미지): tmp/consultation/reservation-{reservationId}/{imageType}/{storedFileName}
	 * - consultation(솔루션 이미지): tmp/consultation/consultation-{consultationId}/solution/{storedFileName}
	 * - review: tmp/review/consultation-{consultationId}/{storedFileName}
	 * - portfolio: tmp/portfolio/expert-{expertId}/{imageType}/{storedFileName}
	 * - expert(전문가 이미지): final/expert/{expertId}/{imageType}/{storedFileName} (바로 final로 업로드)
	 * - storedFileName은 서버에서 UUID 기반으로 생성 (확장자 유지)
	 * 
	 * @param resourceType 리소스 타입 (consultation, review, portfolio, expert 등)
	 * @param resourceId 리소스 ID
	 *   - consultation: 고민지 이미지는 reservationId, 솔루션 이미지는 consultationId
	 *   - review: consultationId
	 *   - portfolio: expertId
	 * @param imageType 이미지 타입
	 *   - 헤어: hairstyle, front, left, right, favorite, difficulty
	 *   - 패션: front, left, right, favorite, purpose
	 *   - 포트폴리오: before, after
	 *   - 전문가 이미지: profile, background
	 *   - 솔루션: (생략 가능, 생략 시 solution으로 처리)
	 * @param fileName 원본 파일명 (확장자 추출용, 예: image.jpg, 1.jpg)
	 * @return Presigned URL과 S3 Key
	 */
	public GeneratePresignedUrlResponse generateUploadPresignedUrl(String resourceType, Long resourceId, String imageType, String fileName) {
		Long userId = getCurrentUserId();

		validateResourceType(resourceType);
		String normalizedImageType = normalizeImageType(resourceType, imageType);
		validateImageType(resourceType, normalizedImageType);
		validateFileName(fileName);

		String storedFileName = generateUuidFileNamePreservingExtension(fileName);

		// expert는 단일 이미지이므로 tmp 없이 final로 바로 업로드 (오너십 검증 포함)
		final String s3Key;
		if ("expert".equals(resourceType)) {
			if (!userId.equals(resourceId)) {
				throw new IllegalArgumentException("본인 전문가 이미지만 업로드할 수 있습니다.");
			}
			s3Key = String.format("final/expert/%d/%s/%s", resourceId, normalizedImageType, storedFileName);
		} else {
			s3Key = buildTemporaryS3Key(userId, resourceType, String.valueOf(resourceId), normalizedImageType, storedFileName);
		}

		log.debug("Presigned URL 발급 - 사용자: {}, S3 Key: {}", userId, s3Key);

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(s3Key)
			.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(Duration.ofMinutes(15))
			.putObjectRequest(putObjectRequest)
			.build();

		String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();

		return GeneratePresignedUrlResponse.builder()
			.s3Key(s3Key)
			.uploadUrl(uploadUrl)
			.expiresIn(900L)
			.build();
	}

	/**
	 * 임시 저장 경로 생성
	 * tmp/{resourceType}/{ownerPrefix}-{resourceId}/{imageType?}/{storedFileName}
	 */
	private String buildTemporaryS3Key(Long userId, String resourceType, String resourceId, String imageType, String fileName) {
		// review는 imageType을 사용하지 않음
		if ("review".equals(resourceType)) {
			return String.format("tmp/review/consultation-%s/%s", resourceId, fileName);
		}

		// consultation의 solution 이미지는 consultationId 기준으로 관리
		if ("consultation".equals(resourceType) && "solution".equals(imageType)) {
			return String.format("tmp/consultation/consultation-%s/solution/%s", resourceId, fileName);
		}

		// consultation(고민지 이미지 등): reservationId 기준
		if ("consultation".equals(resourceType)) {
			return String.format("tmp/consultation/reservation-%s/%s/%s", resourceId, imageType, fileName);
		}

		// portfolio: expertId 기준
		if ("portfolio".equals(resourceType)) {
			return String.format("tmp/portfolio/expert-%s/%s/%s", resourceId, imageType, fileName);
		}

		throw new IllegalArgumentException("허용되지 않는 리소스 타입입니다. (consultation, review, portfolio, expert만 가능)");
	}

	/**
	 * 업로드 시 저장용 파일명 생성 (UUID + 원본 확장자 유지)
	 *
	 * 예: "한글 파일명.PNG" -> "{uuid}.png"
	 */
	private String generateUuidFileNamePreservingExtension(String originalFileName) {
		// validateFileName()에서 null/empty 및 확장자 유효성은 이미 검증됨
		String trimmed = originalFileName.trim();
		int lastDotIndex = trimmed.lastIndexOf('.');
		String extension = trimmed.substring(lastDotIndex + 1).toLowerCase();
		return UUID.randomUUID() + "." + extension;
	}

	/**
	 * Presigned URL 발급 (다운로드용)
	 * 
	 * 경로 구조: final/consultation/{consultationId}/{imageType}/{fileName}
	 * 
	 * @param resourceType 리소스 타입 (consultation, review 등)
	 * @param imageType 이미지 타입
	 * @param resourceId 리소스 ID (consultation ID, review ID 등)
	 * @param fileName 파일명
	 * @return Presigned URL과 S3 Key
	 */
	public GeneratePresignedUrlResponse generateDownloadPresignedUrl(String resourceType, String imageType, Long resourceId, String fileName) {
		validateResourceType(resourceType);
		String normalizedImageType = normalizeImageType(resourceType, imageType);
		validateImageType(resourceType, normalizedImageType);
		validateFileName(fileName);

		// S3 Key 생성 - imageType에 따라 경로 구조가 다름
		String s3Key = buildFinalS3Key(resourceType, normalizedImageType, resourceId, fileName);

		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
			.bucket(bucketName)
			.key(s3Key)
			.build();

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
			.signatureDuration(Duration.ofHours(1))
			.getObjectRequest(getObjectRequest)
			.build();

		String downloadUrl = s3Presigner.presignGetObject(presignRequest).url().toString();

		return GeneratePresignedUrlResponse.builder()
			.s3Key(s3Key)
			.downloadUrl(downloadUrl)
			.expiresIn(3600L) // 1시간 = 3600초
			.build();
	}

	/**
	 * 최종 저장 경로 생성
	 * final/consultation/{consultationId}/{imageType}/{fileName}
	 */
	private String buildFinalS3Key(String resourceType, String imageType, Long resourceId, String fileName) {
		// review는 imageType을 사용하지 않음
		if ("review".equals(resourceType)) {
			return String.format("final/%s/%d/%s", resourceType, resourceId, fileName);
		}
		return String.format("final/%s/%d/%s/%s", resourceType, resourceId, imageType, fileName);
	}

	/**
	 * 리소스별 imageType 정규화
	 *
	 * - review: imageType 미사용 (null 반환)
	 * - consultation: imageType이 비어있으면 solution으로 간주
	 * - portfolio: before/after 필수 (검증은 validateImageType에서 처리)
	 */
	private String normalizeImageType(String resourceType, String imageType) {
		if ("review".equals(resourceType)) {
			return null;
		}
		if ("consultation".equals(resourceType) && (imageType == null || imageType.trim().isEmpty())) {
			return "solution";
		}
		return imageType;
	}

	/**
	 * S3에서 임시 이미지를 최종 위치로 이동
	 * 
	 * 프로세스:
	 * 1. 임시 위치의 파일을 최종 위치로 복사
	 * 2. 임시 위치의 파일 삭제 (재시도 로직 포함)
	 * 3. 삭제 실패 시 정리 작업 기록
	 * 
	 * @param tempS3Key 임시 저장 경로 (tmp/{resourceType}/reservation-{resourceId}/{imageType}/{fileName})
	 * @param finalS3Key 최종 저장 경로 (final/{resourceType}/{resourceId}/{imageType}/{fileName})
	 * @throws RuntimeException 복사 실패 또는 삭제 재시도가 완전히 실패한 경우
	 */
	public void moveImageFromTempToFinal(String tempS3Key, String finalS3Key) {
		try {
//			validateResourceType("consultation");

			// 1. 임시 위치의 파일을 최종 위치로 복사
			log.info("S3 파일 복사 시작 - bucket: {}, from: {}, to: {}", bucketName, tempS3Key, finalS3Key);
			
			CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
				.copySource(bucketName + "/" + tempS3Key)
				.destinationBucket(bucketName)
				.destinationKey(finalS3Key)
				.build();

			s3Client.copyObject(copyObjectRequest);
			log.info("S3 파일 복사 완료 - from: {}, to: {}", tempS3Key, finalS3Key);

			// 2. 임시 위치의 파일 삭제 (재시도 로직 포함)
			deleteTemporaryFileWithRetry(tempS3Key, finalS3Key);

			log.info("S3 이미지 이동 완료 - from: {}, to: {}", tempS3Key, finalS3Key);
		} catch (Exception e) {
			log.error("S3 이미지 이동 실패 - bucket: {}, tempS3Key: {}, finalS3Key: {}", bucketName, tempS3Key, finalS3Key, e);
			throw new RuntimeException("S3 이미지 이동 중 오류가 발생했습니다", e);
		}
	}

	/**
	 * S3 객체 삭제 (보상/정리용)
	 *
	 * - 삭제 재시도 수행
	 * - 모두 실패하면 정리 작업을 DB에 기록 (스케줄러가 재처리)
	 *
	 * @param s3Key 삭제할 S3 객체 키
	 */
	public void deleteObjectWithRetryOrEnqueue(String s3Key) {
		if (s3Key == null || s3Key.trim().isEmpty()) {
			return;
		}

		int attempt = 0;
		Exception lastException = null;

		log.info("S3 객체 삭제 시작 - bucket: {}, key: {}", bucketName, s3Key);

		while (attempt < maxRetries) {
			attempt++;
			try {
				DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
					.bucket(bucketName)
					.key(s3Key)
					.build();

				s3Client.deleteObject(deleteObjectRequest);
				log.info("S3 객체 삭제 성공 - bucket: {}, key: {}, attempt: {}/{}",
					bucketName, s3Key, attempt, maxRetries);
				return;
			} catch (Exception e) {
				lastException = e;
				log.warn("S3 객체 삭제 실패 (재시도 가능) - bucket: {}, key: {}, attempt: {}/{}, error: {}",
					bucketName, s3Key, attempt, maxRetries, e.getMessage());

				if (attempt < maxRetries) {
					try {
						Thread.sleep(retryIntervalMs);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						log.warn("재시도 대기 중단됨", ie);
					}
				}
			}
		}

		log.error("S3 객체 삭제 재시도 모두 실패 - 정리 작업 DB에 기록 - bucket: {}, key: {}", bucketName, s3Key);
		persistCleanupTask(s3Key, null, lastException);
	}

	/**
	 * 임시 파일 삭제 - 재시도 로직 포함
	 * 
	 * 삭제 실패 시:
	 * - 로컬 재시도 (최대 3회)
	 * - 모두 실패하면 정리 작업을 DB에 기록
	 * - 스케줄러가 주기적으로 재처리
	 * 
	 * @param tempS3Key 삭제 대상 파일 경로
	 * @param finalS3Key 최종 파일 경로 (참조용)
	 * @throws RuntimeException 모든 재시도 실패 후
	 */
	private void deleteTemporaryFileWithRetry(String tempS3Key, String finalS3Key) {
		int attempt = 0;
		Exception lastException = null;

		log.info("임시 파일 삭제 시작 - bucket: {}, key: {}", bucketName, tempS3Key);

		// 로컬 재시도 루프
		while (attempt < maxRetries) {
			attempt++;
			try {
				DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
					.bucket(bucketName)
					.key(tempS3Key)
					.build();

				s3Client.deleteObject(deleteObjectRequest);
				log.info("임시 파일 삭제 성공 - bucket: {}, key: {}, attempt: {}/{}", 
					bucketName, tempS3Key, attempt, maxRetries);
				return; // 성공 시 메서드 종료

			} catch (Exception e) {
				lastException = e;
				log.warn("임시 파일 삭제 실패 (재시도 가능) - bucket: {}, key: {}, attempt: {}/{}, error: {}", 
					bucketName, tempS3Key, attempt, maxRetries, e.getMessage());

				// 마지막 시도가 아니면 대기 후 재시도
				if (attempt < maxRetries) {
					try {
						Thread.sleep(retryIntervalMs);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						log.warn("재시도 대기 중단됨", ie);
					}
				}
			}
		}

		// 모든 로컬 재시도 실패 → 정리 작업 기록
		log.error("로컬 재시도 모두 실패 - 정리 작업 DB에 기록 - bucket: {}, key: {}", bucketName, tempS3Key);
		persistCleanupTask(tempS3Key, finalS3Key, lastException);

		// 정리 작업이 기록되었음을 호출자에게 알림
		throw new RuntimeException(
			String.format("S3 임시 파일 삭제 재시도 실패 (정리 작업 기록됨) - bucket: %s, key: %s, 최대 재시도: %d",
				bucketName, tempS3Key, maxRetries),
			lastException
		);
	}

	/**
	 * 정리 작업을 DB에 기록
	 * 
	 * 스케줄러가 주기적으로 이 작업들을 조회하여 재처리
	 * 
	 * @param tempS3Key 삭제 대상 파일 경로
	 * @param finalS3Key 최종 파일 경로 (참조용)
	 * @param exception 발생한 예외
	 */
	private void persistCleanupTask(String tempS3Key, String finalS3Key, Exception exception) {
		try {
			S3CleanupTask cleanupTask = S3CleanupTask.builder()
				.bucketName(bucketName)
				.s3Key(tempS3Key)
				.destinationS3Key(finalS3Key)
				.status(CleanupStatus.PENDING)
				.retryCount(0)
				.maxRetries(maxRetries)
				.createdAt(LocalDateTime.now())
				.errorMessage(exception != null ? exception.getMessage() : "초기 삭제 실패")
				.build();

			s3CleanupTaskRepository.save(cleanupTask);
			log.info("정리 작업 기록됨 - id: {}, bucket: {}, key: {}, destination: {}", 
				cleanupTask.getId(), bucketName, tempS3Key, finalS3Key);

		} catch (Exception e) {
			log.error("정리 작업 기록 실패 - bucket: {}, key: {}, error: {}", 
				bucketName, tempS3Key, e.getMessage(), e);
		}
	}

	/**
	 * 현재 로그인한 사용자 ID 가져오기
	 */
	private Long getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
			throw new IllegalArgumentException("사용자 정보를 찾을 수 없습니다.");
		}
		return (Long) authentication.getPrincipal();
	}

	/**
	 * 리소스 타입 검증
	 */
	private void validateResourceType(String resourceType) {
		if (resourceType == null || resourceType.trim().isEmpty()) {
			throw new IllegalArgumentException("리소스 타입은 필수입니다.");
		}
		if (!resourceType.matches("^[a-z]+$")) {
			throw new IllegalArgumentException("리소스 타입은 영문 소문자만 허용됩니다.");
		}
		// 허용된 리소스 타입만 접수
		if (!resourceType.equals("consultation") && !resourceType.equals("review") && !resourceType.equals("portfolio") && !resourceType.equals("expert")) {
			throw new IllegalArgumentException("허용되지 않는 리소스 타입입니다. (consultation, review, portfolio, expert만 가능)");
		}
	}

	/**
	 * 이미지 타입 검증
	 * 
	 * consultation: hairstyle, front, left, right, favorite, difficulty, purpose, solution
	 * review: 사용 안 함
	 * portfolio: before, after
	 */
	private void validateImageType(String resourceType, String imageType) {
		// review는 imageType을 사용하지 않음 (null/blank 허용)
		if ("review".equals(resourceType)) {
			return;
		}
		if (imageType == null || imageType.trim().isEmpty()) {
			throw new IllegalArgumentException("이미지 타입은 필수입니다.");
		}
		if (!imageType.matches("^[a-z0-9\\-]+$")) {
			throw new IllegalArgumentException("이미지 타입은 영문 소문자, 숫자, 하이픈만 허용됩니다.");
		}

		// 허용된 이미지 타입만 접수 (리소스별로 상이)
		final String[] allowedTypes;
		if ("portfolio".equals(resourceType)) {
			allowedTypes = new String[] {"before", "after"};
		} else if ("expert".equals(resourceType)) {
			allowedTypes = new String[] {"profile", "background"};
		} else {
			allowedTypes = new String[] {"hairstyle", "front", "left", "right", "favorite", "difficulty", "purpose", "solution"};
		}
		boolean isValid = false;
		for (String type : allowedTypes) {
			if (imageType.equals(type)) {
				isValid = true;
				break;
			}
		}
		if (!isValid) {
			throw new IllegalArgumentException("허용되지 않는 이미지 타입입니다.");
		}
	}

	/**
	 * 파일명 검증 (경로 조작 공격 방어, 확장자 검증)
	 */
	private void validateFileName(String fileName) {
		if (fileName == null || fileName.trim().isEmpty()) {
			throw new IllegalArgumentException("파일명은 필수입니다.");
		}
		if (fileName.contains("..") || fileName.startsWith("/") || fileName.startsWith("\\")) {
			throw new IllegalArgumentException("파일명에 경로 조작 문자를 포함할 수 없습니다.");
		}
		
		// 파일 확장자 검증 (이미지 파일만 허용)
		String lowerFileName = fileName.toLowerCase();
		if (!lowerFileName.matches(".*\\.(jpg|jpeg|png|gif|webp)$")) {
			throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다 (jpg, jpeg, png, gif, webp)");
		}
	}

	// Response DTO
	@lombok.Getter
	@lombok.Builder
	public static class GeneratePresignedUrlResponse {
		private String s3Key;
		private String uploadUrl;
		private String downloadUrl;
		private Long expiresIn;
	}

	/**
	 * S3 파일의 공개 URL 생성
	 * 
	 * @param s3Key S3 객체 키 (final/solution/{consultationId}/{fileName})
	 * @return 공개 S3 URL
	 */
	public String generateS3Url(String s3Key) {
		if (s3Key == null || s3Key.trim().isEmpty()) {
			return null;
		}
		// S3 URL 형식: https://bucket-name.s3.region.amazonaws.com/key
		return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, awsRegion, s3Key);
	}
}

