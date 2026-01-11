package com.ceos.menual.domain.reservation.controller;

import com.ceos.menual.domain.common.dto.response.CommonResponse;
import com.ceos.menual.domain.reservation.dto.response.ReservationHistoryListResponseDTO;
import com.ceos.menual.domain.reservation.service.ReservationHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "예약 API", description = "상담 예약 관련 엔드포인트")
public class ReservationHistoryController {

    private final ReservationHistoryService reservationHistoryService;

    /**
     * 예약 내역 조회 API
     * - 확정 대기중인 예약 (UNPAID)
     * - 다가오는 예약 (PAID + 미래 일정)
     */
    @Operation(
            summary = "예약 내역 조회",
            description = "확정 대기중인 예약과 다가오는 예약을 조회합니다."
    )
    @GetMapping("/history")
    public ResponseEntity<CommonResponse<ReservationHistoryListResponseDTO>> getReservationHistory(
            @AuthenticationPrincipal Long userId
    ) {
        ReservationHistoryListResponseDTO response = reservationHistoryService.getReservationHistory(userId);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}