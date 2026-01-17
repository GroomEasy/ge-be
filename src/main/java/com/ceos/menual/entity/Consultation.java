package com.ceos.menual.entity;

import java.time.LocalDateTime;

import com.ceos.menual.entity.enums.ConsultationStatus;
import com.ceos.menual.entity.enums.ConsultationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import lombok.AccessLevel;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "consultations")
public class Consultation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "expert_profile_id", nullable = false)
	private ExpertProfile expertProfile;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "general_profile_id", nullable = false)
	private GeneralProfile generalProfile;

	@Enumerated(EnumType.STRING)
	private ConsultationType type;

	@Enumerated(EnumType.STRING)
	private ConsultationStatus status;

	// 실제 상담 일정
	private LocalDateTime scheduleTime;

	// 화상 상담 관련
	private LocalDateTime videoStartTime;
	private LocalDateTime videoEndTime;
	private Integer durationMinutes;
	private String videoLink;

	// Zoom 미팅 관련 (자동 생성)
	private Long zoomMeetingId;

	@Column(columnDefinition = "TEXT")
	private String zoomJoinUrl;

	private Boolean isZoomMeetingCreated;

	//솔루션 관련
	@Column(columnDefinition = "TEXT")
	private String solution;

	private LocalDateTime solutionSubmittedAt;

	private Boolean reviewWritten;

	/**
	 * 솔루션 업데이트 - 비즈니스 로직을 통한 필드 변경
	 */
	public void updateSolution(String solution) {
		if (solution != null && !solution.trim().isEmpty()) {
			this.solution = solution;
			this.solutionSubmittedAt = LocalDateTime.now();
		}
	}

    // ====== 헬퍼 메서드 ======= //
    /**
     * 후기 작성 완료 처리
     */
    public void markReviewAsWritten() {
        this.reviewWritten = true;
    }

	public void attachZoomMeeting(Long meetingId, String joinUrl) {
		this.zoomMeetingId = meetingId;
		this.zoomJoinUrl = joinUrl;
		this.isZoomMeetingCreated = true;
	}

	/**
	 * 상담 거절 처리
	 */
	public void reject() {
		this.status = ConsultationStatus.REJECTED;
	}
}
