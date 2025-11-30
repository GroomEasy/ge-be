package com.ceos.menual.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "reviews")
public class Review extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "consultation_id")
	private Consultation consultation;

	private Integer rating;

	private String content;

	private String mediaUrls;

	// 카테고리 ID 캐싱
	private Long categoryId;

	@Builder.Default
	private Integer likeCount = 0;

	public void incrementLikeCount() {
		this.likeCount++;
	}

	public void decrementLikeCount() {
		if(this.likeCount > 0){
			this.likeCount--;
		}
	}

}
