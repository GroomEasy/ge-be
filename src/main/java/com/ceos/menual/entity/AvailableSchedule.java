package com.ceos.menual.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "available_schedules",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_expert_date_time",
                        columnNames = {"expert_profile_id", "available_date", "available_time"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AvailableSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_profile_id", nullable = false)
    private ExpertProfile expertProfile;

    // 예약 가능 날짜
    @Column(name = "available_date", nullable = false)
    private LocalDate availableDate;

    // 예약 가능 시간
    @Column(name = "available_time", nullable = false)
    private LocalTime availableTime;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // 비즈니스 메서드
    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}