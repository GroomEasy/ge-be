package com.ceos.menual.domain.reservation.dto.response;

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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryResponseDTO {

    private Long reservationId;             // 예약 ID
    private LocalDateTime confirmedDate;    // 결제 확인 날짜
    private String expertNickname;          // 전문가 닉네임
    private Category category;              // 카테고리 (HAIR, FASHION, SKIN, MAKEUP)
    private ConsultationType consultationType; // 상담 타입 (VIDEO, MESSAGE)
    private Integer cost;                   // 비용 (최종 결제 금액)
    private ReservationStatus status;       // 현재 결제 상태 (PAID, REFUND_REQUESTED, REFUNDED)

    public static PaymentHistoryResponseDTO from(Reservation reservation) {
        return PaymentHistoryResponseDTO.builder()
                .reservationId(reservation.getId())
                .confirmedDate(reservation.getPaidAt() != null ? reservation.getPaidAt() : reservation.getUpdatedAt())
                .expertNickname(reservation.getExpertProfile().getUser().getNickname())
                .category(reservation.getCategory())
                .consultationType(reservation.getConsultationType())
                .cost(reservation.getFinalPrice() != null ? reservation.getFinalPrice() : reservation.getPrice())
                .status(reservation.getReservationStatus())
                .build();
    }
}