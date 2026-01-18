package com.ceos.menual.entity;

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
@Table(name = "general_profile")
public class GeneralProfile extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(nullable = false)
	@Builder.Default
	private Integer totalPoints = 0;

	public void addPoints(int points) {
		if (this.totalPoints == null) {
			this.totalPoints = 0;
		}
		this.totalPoints += points;
	}

	public void deductPoints(int points) {
		if (this.totalPoints == null) {
			this.totalPoints = 0;
		}
		this.totalPoints -= points;
	}

}
