package com.ceos.menual.entity;

import com.ceos.menual.entity.enums.Category;
import jakarta.persistence.*;
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

}
