package com.ceos.menual.domain.reservation.dto.response;

import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.entity.enums.ConsultationType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReservationSheetResponseDTO {

    // 전문가 및 상담 상품 정보
    private ReservationTargetInfo targetInfo;

    // 예약자(결제자) 정보 및 포인트
    private PayerInfo payerInfo;

    // 결제 계좌 정보
    private PaymentAccountInfo accountInfo;


    /* ================= Inner Classes ================= */

    @Getter
    @Builder
    public static class ReservationTargetInfo {
        private String expertNickname;
        private Category category;          // 전문가 카테고리

        private ConsultationType consultationType; // VIDEO or MESSAGE

        private Long originalPrice;         // 정가 (서버 DB 기준)
    }

    @Getter
    @Builder
    public static class PayerInfo {
        private String userNickname;        // 예약자 이름
        private Long totalPoints;       // 현재 보유 포인트
    }

    @Getter
    @Builder
    public static class PaymentAccountInfo {
        private String bankName;        // 은행명
        private String accountNumber;   // 계좌번호
        private String accountHolder;   // 예금주
    }
}