package com.ceos.menual.domain.expert.repository;

import com.ceos.menual.entity.ExpertLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExpertLikeRepository extends JpaRepository<ExpertLike, Long> {

    // 전문가 찜 API 용
    boolean existsByGeneralProfileIdAndExpertProfileId(Long generalProfileId, Long expertProfileId);

    Optional<ExpertLike> findByGeneralProfileIdAndExpertProfileId(Long generalProfileId, Long expertProfileId);

    long countByExpertProfileId(Long expertProfileId);
}
