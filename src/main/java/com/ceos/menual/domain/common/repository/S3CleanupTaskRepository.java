package com.ceos.menual.domain.common.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ceos.menual.domain.common.entity.S3CleanupTask;
import com.ceos.menual.domain.common.entity.S3CleanupTask.CleanupStatus;

/**
 * S3 정리 작업 저장소
 */
@Repository
public interface S3CleanupTaskRepository extends JpaRepository<S3CleanupTask, Long> {

	/**
	 * 재시도 가능한 미처리 작업 조회 (상태: PENDING 또는 IN_PROGRESS)
	 */
	@Query("SELECT t FROM S3CleanupTask t WHERE (t.status = 'PENDING' OR t.status = 'IN_PROGRESS') AND t.retryCount < t.maxRetries ORDER BY t.createdAt ASC")
	List<S3CleanupTask> findRetryableTasks();

	/**
	 * 지정된 기간 내에 실패한 작업 조회
	 */
	@Query("SELECT t FROM S3CleanupTask t WHERE t.status = :status AND t.createdAt >= :startTime ORDER BY t.createdAt DESC")
	List<S3CleanupTask> findByStatusAndCreatedAtAfter(@Param("status") CleanupStatus status, @Param("startTime") LocalDateTime startTime);

	/**
	 * S3 키로 미완료 작업 조회
	 */
	@Query("SELECT t FROM S3CleanupTask t WHERE t.s3Key = :s3Key AND t.status != com.ceos.menual.domain.common.entity.S3CleanupTask$CleanupStatus.COMPLETED")
	List<S3CleanupTask> findPendingByS3Key(@Param("s3Key") String s3Key);

	/**
	 * 오래된 완료된 작업 조회 (7일 이상 전)
	 */
	@Query("SELECT t FROM S3CleanupTask t WHERE t.status = com.ceos.menual.domain.common.entity.S3CleanupTask$CleanupStatus.COMPLETED AND t.completedAt <= :olderThanTime ORDER BY t.completedAt ASC")
	List<S3CleanupTask> findOldCompletedTasks(@Param("olderThanTime") LocalDateTime olderThanTime);
}

