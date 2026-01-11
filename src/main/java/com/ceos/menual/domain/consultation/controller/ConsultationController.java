package com.ceos.menual.domain.consultation.controller;

import com.ceos.menual.domain.common.dto.response.CommonResponse;
import com.ceos.menual.domain.consultation.service.ConsultationService;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.User;
import com.ceos.menual.global.exception.GlobalErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/consults")
@RequiredArgsConstructor
@Tag(name = "Consultation", description = "상담 관련 API")
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
            @RequestBody String solution,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        log.debug("POST 요청 - 사용자 ID: {}, 상담 ID: {}", userId, consultationId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        Long expertProfileId = user.getExpertProfile().getId();
        consultationService.saveSolution(consultationId, solution, expertProfileId);
        
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
        
        String userType = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.equals("ROLE_EXPERT") || auth.equals("ROLE_MEMBER"))
                .map(auth -> auth.replace("ROLE_", ""))
                .findFirst()
                .orElse("MEMBER");
        
        log.debug("사용자 타입: {}, 상담 ID: {}", userType, consultationId);

        String solution = consultationService.getSolution(consultationId, userId, userType);
        return ResponseEntity.ok(CommonResponse.success(solution));
    }
}
