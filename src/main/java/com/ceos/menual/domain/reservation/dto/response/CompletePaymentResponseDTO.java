package com.ceos.menual.domain.reservation.dto.response;

import com.ceos.menual.entity.Consultation;
import com.ceos.menual.entity.enums.ConsultationStatus;
import com.ceos.menual.entity.enums.ConsultationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "결제 완료 응답 (Consultation 생성됨)")
@Getter
@Builder
public class CompletePaymentResponseDTO {

    @Schema(description = "상담 ID", example = "1")
    private Long consultationId;

    @Schema(description = "예약 ID", example = "1")
    private Long reservationId;

    @Schema(description = "상담 유형", example = "VIDEO")
    private ConsultationType type;

    @Schema(description = "상담 상태", example = "READY")
    private ConsultationStatus status;

    @Schema(description = "상담 예정 시간", example = "2026-01-28T11:30:00")
    private LocalDateTime scheduleTime;

    @Schema(description = "메시지", example = "결제가 완료되었으며 상담이 생성되었습니다")
    private String message;

    public static CompletePaymentResponseDTO from(Consultation consultation, Long reservationId) {
        return CompletePaymentResponseDTO.builder()
                .consultationId(consultation.getId())
                .reservationId(reservationId)
                .type(consultation.getType())
                .status(consultation.getStatus())
                .scheduleTime(consultation.getScheduleTime())
                .message("결제가 완료되었으며 상담이 생성되었습니다")
                .build();
    }
}



