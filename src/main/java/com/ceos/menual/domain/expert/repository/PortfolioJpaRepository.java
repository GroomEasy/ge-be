package com.ceos.menual.domain.expert.repository;

import com.ceos.menual.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioJpaRepository extends JpaRepository<Portfolio, Long> {
}

