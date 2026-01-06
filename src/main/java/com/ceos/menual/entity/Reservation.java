package com.ceos.menual.entity;

import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.entity.enums.ConsultationType;
import com.ceos.menual.entity.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_profile_id", nullable = false)
    private ExpertProfile expertProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "general_profile_id", nullable = false)
    private GeneralProfile generalProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_type", nullable = false, length = 20)
    private ConsultationType consultationType;

    // 예약 일정
    @Column(name = "scheduled_date_time")
    private LocalDateTime scheduledDateTime;

    @Column(nullable = false)
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status", nullable = false, length = 30)
    private ReservationStatus reservationStatus;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // 결제 완료 후 생성
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id")
    private Consultation consultation;

    // 고민 입력
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "concerns_json", columnDefinition = "json")
    private String concernsJson;

    // 비즈니스 메서드
    public void updateConcerns(String concernsJson) {
        this.concernsJson = concernsJson;
    }

    public void updateStatusToPaid(Consultation consultation) {
        this.reservationStatus = ReservationStatus.PAID;
        this.consultation = consultation;
        this.expiresAt = null;
    }

    public void cancel() {
        this.reservationStatus = ReservationStatus.CANCELLED;
    }

    public void requestRefund() {
        this.reservationStatus = ReservationStatus.REFUND_REQUESTED;
    }

    public void refund() {
        this.reservationStatus = ReservationStatus.REFUNDED;
    }

    public boolean isExpired() {
        return reservationStatus == ReservationStatus.UNPAID
                && expiresAt != null
                && LocalDateTime.now().isAfter(expiresAt);
    }
}