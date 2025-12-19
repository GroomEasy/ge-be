package com.ceos.menual.entity;

import java.time.LocalDateTime;

import com.ceos.menual.entity.enums.ConsultationStatus;
import com.ceos.menual.entity.enums.ConsultationType;
import com.ceos.menual.entity.enums.PaymentStatus;

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

@Entity
@Getter
@NoArgsConstructor
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

	private LocalDateTime scheduleTime;

	private Integer price;

	@Enumerated(EnumType.STRING)
	private PaymentStatus paymentStatus;

	//화상상담 관련
	private LocalDateTime videoStartTime;
	private LocalDateTime videoEndTime;
	private Integer durationMinutes;
	private String videoLink;


	//고민지 관련 추후 작성 예정

	//솔루션 관련
	private String solutionPdfUrl;
	private LocalDateTime solutionSubmittedAt;

	private Boolean reviewWritten;

}
