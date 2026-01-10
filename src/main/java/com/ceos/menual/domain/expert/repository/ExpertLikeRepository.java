package com.ceos.menual.domain.expert.repository;

import com.ceos.menual.entity.ExpertLike;
import com.ceos.menual.entity.ExpertProfile;
import com.ceos.menual.entity.GeneralProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExpertLikeRepository extends JpaRepository<ExpertLike, Long> {

    // 전문가 찜 API 용
    boolean existsByGeneralProfileAndExpertProfile(GeneralProfile generalProfile, ExpertProfile expertProfile);

    Optional<ExpertLike> findByGeneralProfileAndExpertProfile(GeneralProfile generalProfile, ExpertProfile expertProfile);

    long countByExpertProfileId(Long expertProfileId);

    // 사용자(GeneralProfile)가 찜한 전문가 수 조회
    long countByGeneralProfileId(Long generalProfileId);
}
