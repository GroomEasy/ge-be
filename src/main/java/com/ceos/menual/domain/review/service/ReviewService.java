package com.ceos.menual.domain.review.service;

import java.util.List;

import com.ceos.menual.entity.enums.Category;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ceos.menual.domain.review.dto.response.ReviewSummaryResponseDTO;
import com.ceos.menual.domain.review.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

	private final ReviewRepository reviewRepository;

	public List<ReviewSummaryResponseDTO> getRecentReviews(Category category) {
		return reviewRepository.findRecentReviews(category);
	}

	public List<ReviewSummaryResponseDTO> getBestReviews(Category category) {
		return reviewRepository.findBestReviews(category);
	}
}
