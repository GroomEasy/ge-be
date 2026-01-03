package com.ceos.menual.entity;

import com.ceos.menual.entity.enums.ConsultationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Entity
@Table(name = "consultation_schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ConsultationSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_profile_id", nullable = false)
    @Setter
    private ExpertProfile expertProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsultationType consultationType;  // VIDEO, MESSAGE

    @Positive(message = "가격은 0보다 커야 합니다")
    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;  // 활성화 여부



    // 비즈니스 메서드
    public void updatePrice(Integer newPrice) {
        this.price = newPrice;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}