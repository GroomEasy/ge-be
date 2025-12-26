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
@Table(name = "expert_profile")
public class ExpertProfile extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	private Category category;

	@OneToOne(mappedBy = "expertProfile", cascade = CascadeType.ALL, orphanRemoval = true)
	private ExpertBankAccount expertBankAccount;

	//전문분야
	private String speciality;

	//한줄소개
	private String introduction;

	//프로필 링크
	private String profileLink;

	//경력정보
	private String careerInfo;




}
