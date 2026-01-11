package com.ceos.menual.domain.consultation.controller;

import com.ceos.menual.domain.common.dto.response.CommonResponse;
import com.ceos.menual.domain.consultation.dto.response.ConsultationHistoryResponseDTO;
import com.ceos.menual.domain.consultation.service.ConsultationService;
import com.ceos.menual.entity.enums.Category;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
@Tag(name = "상담 API", description = "상담 관련 엔드포인트")
public class ConsultationController {

    private final ConsultationService consultationService;

    /**
     * 지난 상담 내역 조회 API
     */
    @Operation(
            summary = "지난 상담 내역 조회",
            description = "지난 상담 내역을 전체 조회합니다."
    )
    @GetMapping("/history")
    public ResponseEntity<CommonResponse<List<ConsultationHistoryResponseDTO>>> getConsultationHistory(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "카테고리 (선택)")
            @RequestParam(required = false) Category category
    ) {
        List<ConsultationHistoryResponseDTO> response = consultationService.getConsultationHistory(userId, category);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}