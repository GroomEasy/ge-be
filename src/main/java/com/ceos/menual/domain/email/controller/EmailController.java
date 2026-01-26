package com.ceos.menual.domain.email.controller;

import com.ceos.menual.domain.common.dto.response.CommonResponse;
import com.ceos.menual.domain.email.dto.request.EmailVerificationConfirmDTO;
import com.ceos.menual.domain.email.dto.request.EmailVerificationRequestDTO;
import com.ceos.menual.domain.email.dto.response.EmailVerificationResponseDTO;
import com.ceos.menual.domain.email.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email/verification")
@RequiredArgsConstructor
@Tag(name = "이메일 인증 API", description = "이메일 인증 관련 엔드포인트")
public class EmailController {

    private final EmailVerificationService emailVerificationService;

    /**
     * 인증번호 발송
     */
    @Operation(
            summary = "이메일 인증번호 발송",
            description = "입력한 이메일로 6자리 인증번호를 발송합니다. 인증번호는 5분간 유효합니다."
    )
    @PostMapping("/send")
    public ResponseEntity<CommonResponse<EmailVerificationResponseDTO>> sendVerificationCode(
            @Valid @RequestBody EmailVerificationRequestDTO request
    ) {
        EmailVerificationResponseDTO response = emailVerificationService.sendVerificationCode(request);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 인증번호 확인
     */
    @Operation(
            summary = "이메일 인증번호 확인",
            description = "발송된 인증번호와 입력한 인증번호가 일치하는지 확인합니다."
    )
    @PostMapping("/verify")
    public ResponseEntity<CommonResponse<EmailVerificationResponseDTO>> verifyCode(
            @Valid @RequestBody EmailVerificationConfirmDTO request
    ) {
        EmailVerificationResponseDTO response = emailVerificationService.verifyCode(request);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}