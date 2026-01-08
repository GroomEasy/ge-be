package com.ceos.menual.domain.reservation.controller;

import com.ceos.menual.domain.common.dto.response.CommonResponse;
import com.ceos.menual.domain.reservation.dto.request.CreateTempReservationRequestDTO;
import com.ceos.menual.domain.reservation.dto.request.UpdateReservationConcernRequestDTO;
import com.ceos.menual.domain.reservation.dto.response.AvailableDatesResponseDTO;
import com.ceos.menual.domain.reservation.dto.response.AvailableTimesResponseDTO;
import com.ceos.menual.domain.reservation.dto.response.TempReservationResponseDTO;
import com.ceos.menual.domain.reservation.dto.response.UpdateReservationConcernResponseDTO;
import com.ceos.menual.domain.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "예약 API", description = "상담 예약 관련 엔드포인트")
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 월별 예약 가능 날짜 조회 API
     */
    @Operation(
            summary = "월별 예약 가능 날짜 조회",
            description = "특정 전문가의 특정 월에 예약 가능한 날짜 목록을 조회합니다."
    )
    @GetMapping("/dates")
    public ResponseEntity<CommonResponse<AvailableDatesResponseDTO>> getAvailableDates(
            @Parameter(description = "전문가 UserId", required = true, example = "3")
            @RequestParam Long expertId,

            @Parameter(description = "연도", required = true, example = "2026")
            @RequestParam int year,

            @Parameter(description = "월", required = true, example = "1")
            @RequestParam int month
    ) {
        AvailableDatesResponseDTO response = reservationService.getAvailableDates(expertId, year, month);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 일별 예약 가능 시간 조회 API
     */
    @Operation(
            summary = "일별 예약 가능 시간 조회",
            description = "특정 전문가의 특정 날짜에 예약 가능한 시간대 목록을 조회합니다."
    )
    @GetMapping("/times")
    public ResponseEntity<CommonResponse<AvailableTimesResponseDTO>> getAvailableTimes(
            @Parameter(description = "전문가 UserId", required = true, example = "3")
            @RequestParam Long expertId,

            @Parameter(description = "날짜 (yyyy-MM-dd)", required = true, example = "2026-01-28")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        AvailableTimesResponseDTO response = reservationService.getAvailableTimes(expertId, date);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 임시 예약 생성 API
     */
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

    /**
     * 예약 고민지(concernJson) 업데이트 API
     */
    @Operation(
            summary = "예약 고민지 업데이트",
            description = "예약에 고민지(이미지, 추구미, 상담 목적)를 작성하여 저장합니다."
    )
    @PutMapping("/{reservationId}/concern")
    public ResponseEntity<CommonResponse<UpdateReservationConcernResponseDTO>> updateReservationConcern(
            @Parameter(description = "예약 ID", required = true, example = "1")
            @PathVariable Long reservationId,

            @AuthenticationPrincipal Long userId,

            @Valid @RequestBody UpdateReservationConcernRequestDTO request
    ) {
        UpdateReservationConcernResponseDTO response = reservationService.updateReservationConcern(
                reservationId,
                userId,
                request
        );
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}