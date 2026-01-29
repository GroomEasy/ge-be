package com.ceos.menual.domain.reservation.controller;

import com.ceos.menual.domain.common.dto.response.CommonResponse;
import com.ceos.menual.domain.reservation.dto.request.CompletePaymentRequestDTO;
import com.ceos.menual.domain.reservation.dto.response.CompletePaymentResponseDTO;
import com.ceos.menual.domain.reservation.service.ReservationService;
import com.ceos.menual.domain.user.dto.request.ExpertConversionRequestDTO;
import com.ceos.menual.domain.user.dto.response.ExpertConversionResponseDTO;
import com.ceos.menual.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reservations")
@RequiredArgsConstructor
@Tag(name = "관리자 예약 API", description = "관리자 권한이 필요한 예약 관련 엔드포인트")
public class AdminReservationController {

    private final ReservationService reservationService;
    private final UserService userService;

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
            @Valid @RequestBody CompletePaymentRequestDTO request,
            @AuthenticationPrincipal Long adminUserId
    ) {
        CompletePaymentResponseDTO response = reservationService.confirmPaymentByAdmin(
                reservationId,
                request,
                adminUserId
        );
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 관리자: 결제 완료 예약 취소 API
     * 관리자가 승인한 예약(PAID)을 취소합니다.
     * 취소 시 상담을 REJECTED 상태로 변경하고, 관련 채팅방을 비활성화합니다.
     */
    @Operation(
            summary = "결제 완료 예약 취소",
            description = "관리자가 승인한 예약(PAID)을 취소합니다. 상담을 REJECTED로 변경하고 채팅방을 비활성화합니다."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<CommonResponse<Void>> cancelPaidReservation(
            @Parameter(description = "예약 ID", required = true, example = "1")
            @PathVariable Long reservationId,
            @AuthenticationPrincipal Long adminUserId
    ) {
        reservationService.cancelPaidReservation(reservationId, adminUserId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    /**
     * 전문가 전환 (관리자 전용)
     */
    @Operation(
            summary = "전문가 전환",
            description = "관리자가 일반 회원을 전문가로 전환합니다. GeneralProfile이 삭제되고 ExpertProfile이 생성됩니다."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/convert-to-expert")
    public ResponseEntity<CommonResponse<ExpertConversionResponseDTO>> convertToExpert(
            @Parameter(description = "전환할 사용자 이메일", required = true, example = "user@example.com")
            @RequestParam String email,
            @Valid @RequestBody ExpertConversionRequestDTO requestDTO
    ) {
        ExpertConversionResponseDTO response = userService.convertToExpert(email, requestDTO);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}

