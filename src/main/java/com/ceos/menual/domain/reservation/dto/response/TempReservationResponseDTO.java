package com.ceos.menual.domain.reservation.dto.response;

import com.ceos.menual.entity.Reservation;
import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.entity.enums.ConsultationType;
import com.ceos.menual.entity.enums.ReservationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "임시 예약 생성 응답")
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TempReservationResponseDTO {

    @Schema(description = "전문가 ID", example = "1")
    private Long expertId;

    @Schema(description = "상담 카테고리", example = "HAIR")
    private Category category;

    @Schema(description = "상담 유형", example = "VIDEO")
    private ConsultationType consultationType;

    @Schema(description = "예약 날짜 및 시간", example = "2025-10-28T11:30:00")
    private LocalDateTime scheduledDateTime;

    @Schema(description = "고민 내용 JSON (임시 예약 시점에는 null)", example = "null")
    private String concernsJson;

    // TODO: 상담 유형 조회 API 조회 개발 후 삭제
    @Schema(description = "상담 가격", example = "40000")
    private Integer price;

    @Schema(description = "예약 상태", example = "UNPAID")
    private ReservationStatus reservationStatus;

    public static TempReservationResponseDTO from(Reservation reservation) {
        return TempReservationResponseDTO.builder()
                .expertId(reservation.getExpertProfile().getUser().getId())
                .category(reservation.getCategory())
                .consultationType(reservation.getConsultationType())
                .scheduledDateTime(reservation.getScheduledDateTime())
                .concernsJson(reservation.getConcernsJson())
                .price(reservation.getPrice())
                .reservationStatus(reservation.getReservationStatus())
                .build();
    }
}