package com.ceos.menual.domain.consultation.controller;

import com.ceos.menual.domain.common.dto.response.CommonResponse;
import com.ceos.menual.domain.consultation.dto.request.SolutionRequestDTO;
import com.ceos.menual.domain.consultation.exception.ConsultationErrorCode;
import com.ceos.menual.domain.consultation.dto.response.ConsultationHistoryResponseDTO;
import com.ceos.menual.domain.consultation.service.ConsultationService;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.User;
import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.global.exception.GlobalErrorCode;
import com.ceos.menual.global.exception.GlobalException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
@Tag(name = "상담 API", description = "상담 관련 엔드포인트")
public class ConsultationController {

    private final ConsultationService consultationService;
    private final UserRepository userRepository;

    /**
     * 솔루션 저장 - 해당 전문가만 가능
     */
    @PostMapping("/{consultationId}/solution")
    @Operation(summary = "솔루션 저장", description = "상담에 대한 솔루션을 저장합니다 (해당 전문가만 가능)")
    public ResponseEntity<CommonResponse<Void>> saveSolution(
            @PathVariable Long consultationId,
            @Valid @RequestBody SolutionRequestDTO solutionRequestDTO,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        log.debug("POST 요청 - 사용자 ID: {}, 상담 ID: {}", userId, consultationId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        if (user.getExpertProfile() == null) {
            throw new GlobalException(ConsultationErrorCode.EXPERT_PROFILE_NOT_FOUND);
        }

        Long expertProfileId = user.getExpertProfile().getId();
        consultationService.saveSolution(consultationId, solutionRequestDTO, expertProfileId);

        return ResponseEntity.ok(new CommonResponse<>(GlobalErrorCode.SUCCESS));
    }

    /**
     * 솔루션 조회 - 해당 전문가 또는 상담 회원만 가능
     */
    @GetMapping("/{consultationId}/solution")
    @Operation(summary = "솔루션 조회", description = "상담의 솔루션을 조회합니다 (해당 전문가 또는 상담 회원만 가능)")
    public ResponseEntity<CommonResponse<String>> getSolution(
            @PathVariable Long consultationId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        log.debug("요청 사용자 ID: {}", userId);

        // 모든 권한 출력 (디버깅용)
        log.debug("사용자 권한 목록: {}", authentication.getAuthorities());

        String userType = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .peek(auth -> log.debug("검토 중인 권한: {}", auth))  // 각 권한을 로깅
                .filter(auth -> auth.equals("ROLE_EXPERT") || auth.equals("ROLE_MEMBER"))
                .map(auth -> auth.replace("ROLE_", ""))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("사용자 ID: {}는 ROLE_EXPERT 또는 ROLE_MEMBER 역할이 없습니다", userId);
                    return new GlobalException(ConsultationErrorCode.INVALID_USER_ROLE);
                });

        log.debug("사용자 타입: {}, 상담 ID: {}", userType, consultationId);

        String solution = consultationService.getSolution(consultationId, userId, userType);
        return ResponseEntity.ok(CommonResponse.success(solution));
    }

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
