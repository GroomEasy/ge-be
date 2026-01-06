package com.ceos.menual.domain.reservation.controller;

import com.ceos.menual.domain.common.dto.response.CommonResponse;
import com.ceos.menual.domain.reservation.dto.request.CompletePaymentRequestDTO;
import com.ceos.menual.domain.reservation.dto.response.CompletePaymentResponseDTO;
import com.ceos.menual.domain.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reservations")
@RequiredArgsConstructor
@Tag(name = "관리자 예약 API", description = "관리자 권한이 필요한 예약 관련 엔드포인트")
public class AdminReservationController {

    private final ReservationService reservationService;

    /**
     * 관리자: 결제 확인 및 Consultation 생성 API
     * 사용자가 은행 송금으로 입금한 후, 관리자가 확인하면 호출합니다.
     */
    @Operation(
            summary = "결제 확인",
            description = "사용자의 은행 송금을 확인하고 상담(Consultation)을 생성합니다."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{reservationId}/payment/confirm")
    public ResponseEntity<CommonResponse<CompletePaymentResponseDTO>> confirmPaymentByAdmin(
            @Parameter(description = "예약 ID", required = true, example = "1")
            @PathVariable Long reservationId,

            @Valid @RequestBody CompletePaymentRequestDTO request
    ) {
        CompletePaymentResponseDTO response = reservationService.confirmPaymentByAdmin(
                reservationId,
                request
        );
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}

