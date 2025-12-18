package com.ceos.menual.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
	private String problem;

	// 솔루션
	@Column(nullable = false)
	private String solution;

	// 기타 설명
	@Column(nullable = false)
	private String description;

	// 시술 전 이미지
	private String beforeImage;

	// 시술 후 이미지
	private String afterImage;
}
