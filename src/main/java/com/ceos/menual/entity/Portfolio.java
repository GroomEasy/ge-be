package com.ceos.menual.entity;

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
@Table(name = "portfolios")
public class Portfolio extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "expert_profile_id")
	private ExpertProfile expertProfile;

	// 시술명
	@Column(nullable = false)
	private String title;

	// 고민
	@Column(nullable = false)
	private String concern;

	// 솔루션
	@Column(nullable = false)
	private String solution;

	// 시술 전 이미지
	private String beforeImage;

	// 시술 후 이미지
	private String afterImage;

	// 대표 포트폴리오 여부
	@Builder.Default
	@Column(nullable = false)
	private boolean isRepresentative = false;

	@OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<PortfolioHashtag> hashtags = new ArrayList<>();

	// 연관관계 편의 메서드
	public void addHashtag(PortfolioHashtag portfolioHashtag) {
		hashtags.add(portfolioHashtag);
		portfolioHashtag.setPortfolio(this);
	}

	public void removeHashtag(PortfolioHashtag portfolioHashtag) {
		hashtags.remove(portfolioHashtag);
	}

	public void setIsRepresentative(boolean isRepresentative) {
		this.isRepresentative = isRepresentative;
	}
}
