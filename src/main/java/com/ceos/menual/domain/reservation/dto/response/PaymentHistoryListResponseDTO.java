package com.ceos.menual.domain.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryListResponseDTO {

    private Integer listCount;                          // 결제 내역 개수
    private List<PaymentHistoryResponseDTO> payments;   // 결제 내역 리스트
}