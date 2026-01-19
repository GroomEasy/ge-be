package com.ceos.menual.entity;

import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.global.converter.ZoomTokenEncryptor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

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

	// 배경 이미지
	private String backgroundImage;

	//경력정보
	private String careerInfo;

	// ===== Zoom 연동 =====
	private Boolean isZoomConnected;
	private String zoomUserId;

	@Column(columnDefinition = "TEXT")
	@Convert(converter = ZoomTokenEncryptor.class)
	private String zoomAccessToken;

	@Column(columnDefinition = "TEXT")
	@Convert(converter = ZoomTokenEncryptor.class)
	private String zoomRefreshToken;

	private LocalDateTime zoomTokenExpiresAt;


	// 비즈니스 메서드
	public void addConsultationSchedule(ConsultationSchedule schedule) {
		this.consultationSchedules.add(schedule);
		schedule.setExpertProfile(this);
	}

	public void removeConsultationSchedule(ConsultationSchedule schedule) {
		this.consultationSchedules.remove(schedule);
		schedule.setExpertProfile(null);
	}

	public void connectZoom(String zoomUserId, String accessToken, String refreshToken, LocalDateTime tokenExpiresAt) {
		this.zoomUserId = zoomUserId;
		this.zoomAccessToken = accessToken;
		this.zoomRefreshToken = refreshToken;
		this.zoomTokenExpiresAt = tokenExpiresAt;
		this.isZoomConnected = true;
	}

	public void updateZoomAccessToken(String accessToken, LocalDateTime tokenExpiresAt) {
		this.zoomAccessToken = accessToken;
		this.zoomTokenExpiresAt = tokenExpiresAt;
		if (this.isZoomConnected == null) this.isZoomConnected = true;
	}

	public void updateBackgroundImage(String backgroundImage) {
		this.backgroundImage = backgroundImage;
	}

	public void updateIntroduction(String introduction) {
		this.introduction = introduction;
	}

	public void updateProfileLink(String profileLink) {
		this.profileLink = profileLink;
	}

	public void updateCareerInfo(String careerInfo) {
		this.careerInfo = careerInfo;
	}

}
