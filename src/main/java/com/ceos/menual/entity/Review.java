package com.ceos.menual.entity;

import com.ceos.menual.entity.enums.Category;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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

	@OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("displayOrder ASC")
	@Builder.Default
	private List<ReviewImage> images = new ArrayList<>();

	@OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<ReviewHashtag> hashtags = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(name = "category")
	private Category category;

	@Builder.Default
	private Integer likeCount = 0;

	public void incrementLikeCount() {
		if(this.likeCount == null){
			this.likeCount = 0;
		}
		this.likeCount++;
	}

	public void decrementLikeCount() {
		if(this.likeCount == null || this.likeCount == 0){
			return;
		}
		this.likeCount--;
	}

	public void addImage(ReviewImage image) {
		images.add(image);
		image.setReview(this);
	}

	public void removeImage(ReviewImage image) {
		images.remove(image);
	}

	public void addHashtag(ReviewHashtag reviewHashtag) {
		hashtags.add(reviewHashtag);
		reviewHashtag.setReview(this);
	}

	public void removeHashtag(ReviewHashtag reviewHashtag) {
		hashtags.remove(reviewHashtag);
	}
}
