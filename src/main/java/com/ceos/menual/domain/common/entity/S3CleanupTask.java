package com.ceos.menual.domain.common.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * S3 파일 정리 작업 기록
 * - 복사 후 삭제 실패 시 이를 기록하고 나중에 처리
 * - 스케줄러가 주기적으로 미처리 작업을 재시도
 */
@Entity
@Table(name = "s3_cleanup_tasks")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class S3CleanupTask {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * S3 버킷명
	 */
	@Column(nullable = false)
	private String bucketName;

	/**
	 * 삭제 대상 S3 키 (임시 파일 경로)
	 */
	@Column(nullable = false, length = 500)
	private String s3Key;

	/**
	 * 목적지 S3 키 (최종 파일 경로) - 참조용
	 */
	@Column(length = 500)
	private String destinationS3Key;

	/**
	 * 작업 상태
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CleanupStatus status;

	/**
	 * 재시도 횟수
	 */
	@Column(nullable = false)
	private Integer retryCount;

	/**
	 * 최대 재시도 횟수
	 */
	@Column(nullable = false)
	private Integer maxRetries;

	/**
	 * 작업 생성 시간
	 */
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/**
	 * 마지막 재시도 시간
	 */
	@Column
	private LocalDateTime lastRetryAt;

	/**
	 * 완료 시간
	 */
	@Column
	private LocalDateTime completedAt;

	/**
	 * 에러 메시지
	 */
	@Column(columnDefinition = "TEXT")
	private String errorMessage;

	/**
	 * 상태 설정
	 */
	public void setStatus(CleanupStatus status) {
		this.status = status;
	}

	/**
	 * 작업 상태 열거형
	 */
	public enum CleanupStatus {
		PENDING("대기"),
		IN_PROGRESS("처리중"),
		COMPLETED("완료"),
		FAILED("실패");

		private final String description;

		CleanupStatus(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	/**
	 * 재시도 가능 여부 확인
	 */
	public boolean isRetryable() {
		return retryCount < maxRetries && status != CleanupStatus.COMPLETED;
	}

	/**
	 * 재시도 횟수 증가
	 */
	public void incrementRetryCount() {
		this.retryCount++;
		this.lastRetryAt = LocalDateTime.now();
	}

	/**
	 * 작업 완료 처리
	 */
	public void markAsCompleted() {
		this.status = CleanupStatus.COMPLETED;
		this.completedAt = LocalDateTime.now();
	}

	/**
	 * 작업 실패 처리
	 */
	public void markAsFailed(String errorMessage) {
		this.status = CleanupStatus.FAILED;
		this.errorMessage = errorMessage;
	}
}

