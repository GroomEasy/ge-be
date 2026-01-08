package com.ceos.menual.domain.reservation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "관리자: 결제 확인 요청 (Consultation 생성)")
@Getter
@Setter
@NoArgsConstructor
public class CompletePaymentRequestDTO {
    // 결제 확인에 필요한 추가 정보 없음
}

