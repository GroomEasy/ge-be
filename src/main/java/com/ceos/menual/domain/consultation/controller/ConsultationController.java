package com.ceos.menual.domain.consultation.controller;

import com.ceos.menual.domain.common.dto.response.CommonResponse;
import com.ceos.menual.domain.consultation.dto.request.SolutionRequestDTO;
import com.ceos.menual.domain.consultation.dto.response.ConcernResponseDTO;
import com.ceos.menual.domain.consultation.dto.response.ConsultationHistoryResponseDTO;
import com.ceos.menual.domain.consultation.dto.response.ExpertConsultationHistoryResponseDTO;
import com.ceos.menual.domain.consultation.dto.response.SolutionListResponseDTO;
import com.ceos.menual.domain.consultation.exception.ConsultationErrorCode;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
     * 고민지 조회 - 해당 전문가 또는 상담 회원만 가능
     */
    @GetMapping("/{consultationId}/concern")
    @Operation(summary = "고민지 조회", description = "상담의 고민지를 조회합니다 (해당 전문가 또는 상담 회원만 가능)")
    public ResponseEntity<CommonResponse<ConcernResponseDTO>> getConcern(
            @PathVariable Long consultationId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        log.debug("고민지 조회 요청 - 사용자 ID: {}, 상담 ID: {}", userId, consultationId);

        String userType = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .peek(auth -> log.debug("검토 중인 권한: {}", auth))
                .filter(auth -> auth.equals("ROLE_EXPERT") || auth.equals("ROLE_MEMBER"))
                .map(auth -> auth.replace("ROLE_", ""))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("사용자 ID: {}는 ROLE_EXPERT 또는 ROLE_MEMBER 역할이 없습니다", userId);
                    return new GlobalException(ConsultationErrorCode.INVALID_USER_ROLE);
                });

        ConcernResponseDTO response = consultationService.getConcern(consultationId, userId, userType);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 솔루션 저장 - 해당 전문가만 가능
     * 
     * PreAuthorize: ROLE_EXPERT만 접근 가능 (선언적 보안)
     * Safe principal extraction: 문자열 파싱 + null-safe 변환
     * Extra defense: controller 단계에서 expertProfile 검증
     */
    @PostMapping("/{consultationId}/solution")
    @PreAuthorize("hasRole('ROLE_EXPERT')")
    @Operation(summary = "솔루션 저장", description = "상담에 대한 솔루션을 저장합니다 (해당 전문가만 가능)")
    public ResponseEntity<CommonResponse<Void>> saveSolution(
            @PathVariable Long consultationId,
            @Valid @RequestBody SolutionRequestDTO solutionRequestDTO,
            Authentication authentication
    ) {
        // Safe principal extraction: 문자열 파싱 + null-safe 변환
        Long userId;
        try {
            if (authentication == null) {
                log.error("인증 정보가 없습니다 - consultationId: {}", consultationId);
                throw new GlobalException(UserErrorCode.USER_NOT_FOUND);
            }
            
            Object principal = authentication.getPrincipal();
            
            // instanceof 체크 (type-safe)
            if (principal instanceof Long) {
                userId = (Long) principal;
            } else if (principal instanceof String) {
                // 문자열인 경우 parseLong 시도
                try {
                    userId = Long.parseLong((String) principal);
                } catch (NumberFormatException e) {
                    log.error("사용자 ID 파싱 실패 - principal: {}", principal, e);
                    throw new GlobalException(UserErrorCode.USER_NOT_FOUND);
                }
            } else {
                log.error("예상치 못한 principal 타입 - type: {}", principal.getClass().getName());
                throw new GlobalException(UserErrorCode.USER_NOT_FOUND);
            }
        } catch (GlobalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Principal 추출 중 오류 - consultationId: {}", consultationId, e);
            throw new GlobalException(UserErrorCode.USER_NOT_FOUND);
        }

        log.debug("솔루션 저장 요청 - 사용자 ID: {}, 상담 ID: {}", userId, consultationId);

        // DB에서 사용자 확인 (중복 방어)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("사용자를 찾을 수 없습니다 - userId: {}", userId);
                    return new GlobalException(UserErrorCode.USER_NOT_FOUND);
                });

        // 전문가 프로필 확인 (방어 심화)
        if (user.getExpertProfile() == null) {
            log.error("전문가 프로필이 없습니다 - userId: {}, consultationId: {}", userId, consultationId);
            throw new GlobalException(ConsultationErrorCode.EXPERT_PROFILE_NOT_FOUND);
        }

        Long expertProfileId = user.getExpertProfile().getId();
        log.info("솔루션 저장 권한 검증 완료 - userId: {}, expertProfileId: {}, consultationId: {}", 
            userId, expertProfileId, consultationId);

        // Service 호출 (최종 권한 검증은 service에서 수행)
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

    /**
     * 솔루션 목록 조회 API (전체/카테고리별)
     * 솔루션지 조회 버튼 클릭 시 GET /api/consultations/{consultationId}/solution 호출
     */
    @Operation(
            summary = "솔루션 목록 조회",
            description = "사용자가 받은 솔루션 목록을 전체 또는 카테고리별로 조회합니다. 솔루션지 상세 조회는 GET /api/consultations/{consultationId}/solution API를 호출하세요."
    )
    @GetMapping("/solutions")
    public ResponseEntity<CommonResponse<List<SolutionListResponseDTO>>> getSolutionList(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "카테고리 (HAIR, FASHION, MAKEUP, SKIN 중 선택, 미입력 시 전체 조회)")
            @RequestParam(required = false) Category category
    ) {
        List<SolutionListResponseDTO> response = consultationService.getSolutionList(userId, category);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 전문가용 상담 내역 조회 API
     */
    @Operation(
            summary = "전문가용 상담 내역 조회",
            description = "전문가가 진행한 상담 내역을 조회합니다. (전문가만 접근 가능)"
    )
    @PreAuthorize("hasRole('ROLE_EXPERT')")
    @GetMapping("/expert/history")
    public ResponseEntity<CommonResponse<List<ExpertConsultationHistoryResponseDTO>>> getExpertConsultationHistory(
            @AuthenticationPrincipal Long userId
    ) {
        List<ExpertConsultationHistoryResponseDTO> response = consultationService.getExpertConsultationHistory(userId);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
