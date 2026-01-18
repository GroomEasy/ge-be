package com.ceos.menual.domain.user.repository;

import com.ceos.menual.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    /**
     * 사용자의 포인트 히스토리 조회 (최신순)
     */
    List<PointHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
}