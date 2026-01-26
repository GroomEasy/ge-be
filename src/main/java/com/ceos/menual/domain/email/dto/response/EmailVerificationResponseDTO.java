package com.ceos.menual.domain.email.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "이메일 인증 응답")
public class EmailVerificationResponseDTO {

    @Schema(description = "이메일 주소", example = "user@example.com")
    private String email;

    @Schema(description = "인증 성공 여부", example = "true")
    private boolean verified;

    @Schema(description = "메시지", example = "인증번호가 발송되었습니다.")
    private String message;

    public static EmailVerificationResponseDTO codeSent(String email) {
        return EmailVerificationResponseDTO.builder()
                .email(email)
                .verified(false)
                .message("인증번호가 발송되었습니다.")
                .build();
    }

    public static EmailVerificationResponseDTO verified(String email) {
        return EmailVerificationResponseDTO.builder()
                .email(email)
                .verified(true)
                .message("이메일 인증이 완료되었습니다.")
                .build();
    }
}