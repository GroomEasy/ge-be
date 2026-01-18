package com.ceos.menual.domain.reservation.controller;

import com.ceos.menual.domain.common.dto.response.CommonResponse;
import com.ceos.menual.domain.reservation.dto.request.CreateTempReservationRequestDTO;
import com.ceos.menual.domain.reservation.dto.request.UpdateReservationConcernRequestDTO;
import com.ceos.menual.domain.reservation.dto.request.UpdateFashionConcernRequestDTO;
import com.ceos.menual.domain.reservation.dto.request.UpdateHairConcernRequestDTO;
import com.ceos.menual.domain.reservation.dto.response.*;
import com.ceos.menual.domain.reservation.service.ReservationService;
import com.ceos.menual.entity.enums.ConsultationType;
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
     * 패션 상담 고민지 업데이트 API
     */
    @Operation(
            summary = "패션 상담 고민지 업데이트",
            description = "패션 상담에 고민지(이미지, 신체 정보, 스타일 선호)를 작성하여 저장합니다."
    )
    @PutMapping("/{reservationId}/fashion-concern")
    public ResponseEntity<CommonResponse<UpdateReservationConcernResponseDTO>> updateFashionConcern(
            @Parameter(description = "예약 ID", required = true, example = "1")
            @PathVariable Long reservationId,

            @AuthenticationPrincipal Long userId,

            @Valid @RequestBody UpdateFashionConcernRequestDTO request
    ) {
        UpdateReservationConcernResponseDTO response = reservationService.updateFashionConcern(
                reservationId,
                userId,
                request
        );
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 헤어 상담 고민지 업데이트 API
     */
    @Operation(
            summary = "헤어 상담 고민지 업데이트",
            description = "헤어 상담에 고민지(이미지, 얼굴 정보, 스타일 선호)를 작성하여 저장합니다."
    )
    @PutMapping("/{reservationId}/hair-concern")
    public ResponseEntity<CommonResponse<UpdateReservationConcernResponseDTO>> updateHairConcern(
            @Parameter(description = "예약 ID", required = true, example = "1")
            @PathVariable Long reservationId,

            @AuthenticationPrincipal Long userId,

            @Valid @RequestBody UpdateHairConcernRequestDTO request
    ) {
        UpdateReservationConcernResponseDTO response = reservationService.updateHairConcern(
                reservationId,
                userId,
                request
        );
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 임시 예약 취소 API
     * 일반 사용자가 자신의 임시 예약(UNPAID)을 취소합니다.
     */
    @Operation(
            summary = "임시 예약 취소",
            description = "일반 사용자가 자신의 임시 예약(UNPAID)을 취소합니다. 관리자 승인 전에만 취소 가능합니다."
    )
    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<CommonResponse<Void>> cancelTempReservation(
            @Parameter(description = "예약 ID", required = true, example = "1")
            @PathVariable Long reservationId,
            @AuthenticationPrincipal Long userId
    ) {
        reservationService.cancelTempReservation(reservationId, userId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    /**
     * 환불 요청 API
     * 일반 사용자가 자신의 결제 완료 예약(PAID)에 대해 환불을 요청합니다.
     */
    @Operation(
            summary = "환불 요청",
            description = "일반 사용자가 결제 완료된 예약(PAID)에 대해 환불을 요청합니다. 관리자 승인 후 환불 처리됩니다."
    )
    @PostMapping("/{reservationId}/refund/request")
    public ResponseEntity<CommonResponse<Void>> requestRefund(
            @Parameter(description = "예약 ID", required = true, example = "1")
            @PathVariable Long reservationId,
            @AuthenticationPrincipal Long userId
    ) {
        reservationService.requestRefund(reservationId, userId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    /**
     * 예약 주문서(결제 전 확인 페이지) 조회 API
     */
    @Operation(
            summary = "예약 주문서 조회",
            description = "결제 전 단계에서 전문가 정보, 상담 가격(서버 기준), 내 잔여 포인트, 계좌 정보 등을 조회합니다."
    )
    @GetMapping("/sheet")
    public ResponseEntity<CommonResponse<ReservationSheetResponseDTO>> getReservationSheet(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,

            @Parameter(description = "예약하려는 전문가의 ID", required = true, example = "3")
            @RequestParam Long expertId,

            @Parameter(description = "상담 유형 (VIDEO: 화상, MESSAGE: 문자)", required = true, example = "VIDEO")
            @RequestParam ConsultationType type
    ) {
        // 서비스 호출
        ReservationSheetResponseDTO response =
                reservationService.getReservationSheet(userId, expertId, type);

        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 포인트 적용/변경 API
     * 예약에 포인트를 적용하거나 변경합니다.
     */
    @Operation(
            summary = "포인트 적용/변경",
            description = "예약에 포인트를 적용하거나 변경합니다. 전액 사용, 부분 사용, 사용 취소가 가능합니다."
    )
    @PutMapping("/{reservationId}/points")
    public ResponseEntity<CommonResponse<PointApplicationResponseDTO>> applyPoints(
            @Parameter(description = "예약 ID", required = true, example = "1")
            @PathVariable Long reservationId,
            @Valid @RequestBody com.ceos.menual.domain.reservation.dto.request.ApplyPointsRequestDTO request,
            @AuthenticationPrincipal Long userId
    ) {
        PointApplicationResponseDTO response =
                reservationService.applyPoints(reservationId, userId, request.getPointsToUse());
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * Reservation Sheet 제출 API
     * 예약 주문서를 제출하여 입금 대기 상태로 전환합니다.
     */
    @Operation(
            summary = "예약 주문서 제출",
            description = "고민지와 포인트 등을 모두 작성한 후 예약 주문서를 제출합니다. 제출 후에는 입금 대기 상태(SUBMITTED)로 변경되며, 포인트 변경이 불가능합니다."
    )
    @PostMapping("/{reservationId}/submit")
    public ResponseEntity<CommonResponse<Void>> submitReservation(
            @Parameter(description = "예약 ID", required = true, example = "1")
            @PathVariable Long reservationId,
            @AuthenticationPrincipal Long userId
    ) {
        reservationService.submitReservation(reservationId, userId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

}