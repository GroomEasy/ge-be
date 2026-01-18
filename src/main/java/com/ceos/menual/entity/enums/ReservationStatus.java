package com.ceos.menual.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReservationStatus {
    UNPAID("미입금"),                    // 임시 예약 상태
    SUBMITTED("결제 대기"),              // 결제 대기
    PAID("입금 완료"),                   // 결제 완료
    CANCELLED("취소 완료"),              // 미입금 상태에서 취소
    REFUND_REQUESTED("환불 요청"),       // 환불 요청
    REFUNDED("환불 완료");               // 환불 완료

    private final String description;
}