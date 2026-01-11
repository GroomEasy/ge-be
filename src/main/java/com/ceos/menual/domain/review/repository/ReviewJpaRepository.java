package com.ceos.menual.domain.review.repository;

import com.ceos.menual.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewJpaRepository extends JpaRepository<Review, Long> {

}