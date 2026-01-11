package com.ceos.menual.domain.reservation.dto.response;

import com.ceos.menual.domain.reservation.dto.ReservationHistoryDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationHistoryListResponseDTO {

    // 확정 대기중인 예약 (UNPAID)
    private List<ReservationHistoryDTO> unpaidReservations;

    // 다가오는 예약 (PAID + 미래 일정)
    private List<ReservationHistoryDTO> upcomingReservations;

    public static ReservationHistoryListResponseDTO of(
            List<ReservationHistoryDTO> unpaidReservations,
            List<ReservationHistoryDTO> upcomingReservations
    ) {

        return ReservationHistoryListResponseDTO.builder()
                .unpaidReservations(unpaidReservations)
                .upcomingReservations(upcomingReservations)
                .build();
    }
}




