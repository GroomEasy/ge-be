package com.ceos.menual.domain.reservation.controller;

import com.ceos.menual.domain.common.dto.response.CommonResponse;
import com.ceos.menual.domain.reservation.dto.request.CreateTempReservationRequestDTO;
import com.ceos.menual.domain.reservation.dto.response.TempReservationResponseDTO;
import com.ceos.menual.domain.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "예약 API", description = "상담 예약 관련 엔드포인트")
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(
            summary = "임시 예약 생성",
            description = "전문가 상담 임시 예약을 생성합니다."
    )
    @PostMapping("/temp")
    public ResponseEntity<CommonResponse<TempReservationResponseDTO>> createTempReservation(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateTempReservationRequestDTO request
    ) {
        TempReservationResponseDTO response = reservationService.createTempReservation(userId, request);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}