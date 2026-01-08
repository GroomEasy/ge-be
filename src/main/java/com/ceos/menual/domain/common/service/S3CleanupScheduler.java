package com.ceos.menual.domain.common.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ceos.menual.domain.common.entity.S3CleanupTask;
import com.ceos.menual.domain.common.entity.S3CleanupTask.CleanupStatus;
import com.ceos.menual.domain.common.repository.S3CleanupTaskRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

/**
 * S3 정리 작업 스케줄러
 * 
 * 이 스케줄러는 다음을 담당합니다:
 * 1. 미처리 정리 작업 재시도 (15분마다)
 * 2. 완료된 작업 정리 (일 1회)
 * 3. 장시간 PENDING 상태의 작업 모니터링 (1시간마다)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3CleanupScheduler {

	private final S3CleanupTaskRepository s3CleanupTaskRepository;
	private final S3Client s3Client;

	@Value("${aws.s3.bucket-name}")
	private String bucketName;

	@Value("${aws.s3.cleanup.retry-interval-ms:1000}")
	private Long retryIntervalMs;

	/**
	 * 미처리된 정리 작업 재시도 (15분마다 실행)
	 */
	@Scheduled(fixedRateString = "${aws.s3.cleanup.retry-scheduler-interval:900000}", initialDelayString = "${aws.s3.cleanup.initial-delay:60000}")
	@Transactional
	public void retryCleanupTasks() {
		log.info("[S3 정리 스케줄러] 미처리 정리 작업 재시도 시작");

		List<S3CleanupTask> retryableTasks = s3CleanupTaskRepository.findRetryableTasks();

		if (retryableTasks.isEmpty()) {
			log.info("[S3 정리 스케줄러] 재시도할 작업 없음");
			return;
		}

		log.info("[S3 정리 스케줄러] 재시도 대상 작업 수: {}", retryableTasks.size());

		for (S3CleanupTask task : retryableTasks) {
			processCleanupTask(task);
		}

		log.info("[S3 정리 스케줄러] 미처리 정리 작업 재시도 완료");
	}

	/**
	 * 개별 정리 작업 처리
	 */
	private void processCleanupTask(S3CleanupTask task) {
		try {
			log.info("[S3 정리 작업] 처리 시작 - id: {}, s3Key: {}, retryCount: {}/{}", 
				task.getId(), task.getS3Key(), task.getRetryCount(), task.getMaxRetries());

			// 작업 상태를 IN_PROGRESS로 변경
			task.incrementRetryCount();
			task.setStatus(CleanupStatus.IN_PROGRESS);
			s3CleanupTaskRepository.save(task);

			// S3 파일 삭제 시도
			DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
				.bucket(task.getBucketName())
				.key(task.getS3Key())
				.build();

			s3Client.deleteObject(deleteObjectRequest);

			// 성공 시 작업 완료 처리
			task.markAsCompleted();
			s3CleanupTaskRepository.save(task);

			log.info("[S3 정리 작업] 완료 - id: {}, s3Key: {}, 재시도 횟수: {}", 
				task.getId(), task.getS3Key(), task.getRetryCount());

		} catch (Exception e) {
			log.error("[S3 정리 작업] 실패 - id: {}, s3Key: {}, bucket: {}, error: {}", 
				task.getId(), task.getS3Key(), task.getBucketName(), e.getMessage(), e);

			// 재시도 가능 여부 확인
			if (task.isRetryable()) {
				task.setStatus(CleanupStatus.PENDING);
				log.warn("[S3 정리 작업] 다음 재시도 예정 - id: {}, 다음 재시도 횟수: {}/{}", 
					task.getId(), task.getRetryCount() + 1, task.getMaxRetries());
			} else {
				task.markAsFailed(e.getMessage());
				log.error("[S3 정리 작업] 최대 재시도 초과, 실패 처리 - id: {}, s3Key: {}", 
					task.getId(), task.getS3Key());
			}

			s3CleanupTaskRepository.save(task);
		}

		// 재시도 간격 대기
		try {
			Thread.sleep(retryIntervalMs);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("[S3 정리 스케줄러] 재시도 대기 중단됨", e);
		}
	}

	/**
	 * 완료된 작업 정리 (7일 이상 전 완료된 작업 삭제)
	 * 하루에 1회, 새벽 3시에 실행
	 */
	@Scheduled(cron = "${aws.s3.cleanup.old-task-cleanup-cron:0 0 3 * * *}")
	@Transactional
	public void cleanupOldCompletedTasks() {
		log.info("[S3 정리 스케줄러] 오래된 완료 작업 정리 시작");

		LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
		List<S3CleanupTask> oldTasks = s3CleanupTaskRepository.findOldCompletedTasks(sevenDaysAgo);

		if (oldTasks.isEmpty()) {
			log.info("[S3 정리 스케줄러] 삭제할 오래된 작업 없음");
			return;
		}

		log.info("[S3 정리 스케줄러] 오래된 완료 작업 삭제 중 - 삭제 대상: {}개", oldTasks.size());

		s3CleanupTaskRepository.deleteAll(oldTasks);

		log.info("[S3 정리 스케줄러] 오래된 완료 작업 정리 완료 - 삭제됨: {}개", oldTasks.size());
	}

	/**
	 * 장시간 PENDING 상태의 작업 모니터링 (1시간마다 실행)
	 * 
	 * 오래 처리되지 않는 작업이 있으면 알림 로그 기록
	 */
	@Scheduled(fixedRateString = "${aws.s3.cleanup.stale-task-check-interval:3600000}", initialDelayString = "${aws.s3.cleanup.initial-delay:60000}")
	public void monitorStaleTasks() {
		log.info("[S3 정리 스케줄러] 장시간 미처리 작업 모니터링 시작");

		LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
		List<S3CleanupTask> staleTasks = s3CleanupTaskRepository.findByStatusAndCreatedAtAfter(
			CleanupStatus.PENDING, oneHourAgo);

		if (staleTasks.isEmpty()) {
			log.debug("[S3 정리 스케줄러] 장시간 미처리 작업 없음");
			return;
		}

		log.warn("[S3 정리 스케줄러] 1시간 이상 PENDING 상태인 작업 발견 - 작업 수: {}", staleTasks.size());

		for (S3CleanupTask task : staleTasks) {
			log.warn("[S3 정리 스케줄러] 장시간 미처리 작업 - id: {}, s3Key: {}, createdAt: {}, retryCount: {}/{}", 
				task.getId(), task.getS3Key(), task.getCreatedAt(), task.getRetryCount(), task.getMaxRetries());
		}
	}
}

