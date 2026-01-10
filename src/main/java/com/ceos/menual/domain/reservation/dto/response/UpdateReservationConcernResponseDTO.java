package com.ceos.menual.domain.reservation.dto.response;

import com.ceos.menual.entity.Reservation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "예약 고민지 업데이트 응답")
@Getter
@Builder
public class UpdateReservationConcernResponseDTO {

    @Schema(description = "예약 ID", example = "1")
    private Long reservationId;

    @Schema(description = "업데이트된 고민지")
    private Object concern;

    @Schema(description = "성공 메시지", example = "고민지가 성공적으로 저장되었습니다")
    private String message;

    public static UpdateReservationConcernResponseDTO from(Reservation reservation, Object concernJson) {
        return UpdateReservationConcernResponseDTO.builder()
                .reservationId(reservation.getId())
                .concern(concernJson)
                .message("고민지가 성공적으로 저장되었습니다")
                .build();
    }
}



