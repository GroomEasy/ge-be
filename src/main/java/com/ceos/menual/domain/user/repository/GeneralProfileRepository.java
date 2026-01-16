package com.ceos.menual.domain.user.repository;

import com.ceos.menual.entity.GeneralProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneralProfileRepository extends JpaRepository<GeneralProfile, Long> {
}