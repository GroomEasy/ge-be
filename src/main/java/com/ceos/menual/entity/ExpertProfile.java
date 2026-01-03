package com.ceos.menual.entity;

import com.ceos.menual.entity.enums.Category;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

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

	// 전문가가 제공하는 상담 서비스들
	@OneToMany(mappedBy = "expertProfile", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<ConsultationSchedule> consultationSchedules = new ArrayList<>();

	// 전문 분야
	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(
			name = "expert_speciality",
			joinColumns = @JoinColumn(name = "expert_profile_id")
	)
	@Column(name = "speciality")
	@BatchSize(size = 100)
	@Builder.Default
	private List<String> specialities = new ArrayList<>();

	//한줄소개
	private String introduction;

	//프로필 링크
	private String profileLink;

	//경력정보
	private String careerInfo;


	// 비즈니스 메서드
	public void addConsultationSchedule(ConsultationSchedule schedule) {
		this.consultationSchedules.add(schedule);
		schedule.setExpertProfile(this);
	}

	public void removeConsultationSchedule(ConsultationSchedule schedule) {
		this.consultationSchedules.remove(schedule);
		schedule.setExpertProfile(null);
	}

}
