package com.ceos.menual.domain.reservation.dto;

import com.ceos.menual.entity.Reservation;
import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.entity.enums.ConsultationType;
import com.ceos.menual.entity.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationHistoryDTO {

    private Long reservationId;
    private Long consultationId;

    // 전문가 정보
    private String expertName;
    private String expertProfileImage;
    private Category category;

    // 예약 정보
    private ConsultationType consultationType;
    private LocalDateTime scheduledDateTime;
    private Integer price;
    private ReservationStatus status;

    // 상태 판단용
    private Boolean isPast; // 지난 예약인지
    private Boolean canCancel; // 예약취소 가능 여부
    private Boolean canRequestRefund; // 예약취소(환불요청) 가능 여부

    private LocalDateTime createdAt;

    public static ReservationHistoryDTO from(Reservation reservation) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledDateTime = reservation.getScheduledDateTime();
        ReservationStatus status = reservation.getReservationStatus();

        // 지난 예약 판단: 예약 시간이 과거인 경우
        boolean isPast = scheduledDateTime != null && scheduledDateTime.isBefore(now);

        // 예약취소 가능: UNPAID 상태인 경우 (미입금)
        boolean canCancel = status == ReservationStatus.UNPAID;

        // 환불요청 가능: PAID 상태이면서 예약 시간이 아직 안 지난 경우
        boolean canRequestRefund = status == ReservationStatus.PAID && !isPast;

        return ReservationHistoryDTO.builder()
                .reservationId(reservation.getId())
                .consultationId(reservation.getConsultation() != null ? reservation.getConsultation().getId() : null)
                .expertName(reservation.getExpertProfile().getUser().getNickname())
                .expertProfileImage(reservation.getExpertProfile().getUser().getProfileImage())
                .category(reservation.getCategory())
                .consultationType(reservation.getConsultationType())
                .scheduledDateTime(scheduledDateTime)
                .price(reservation.getPrice())
                .status(status)
                .isPast(isPast)
                .canCancel(canCancel)
                .canRequestRefund(canRequestRefund)
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}