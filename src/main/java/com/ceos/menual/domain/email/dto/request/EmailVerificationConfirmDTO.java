package com.ceos.menual.domain.email.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "이메일 인증번호 확인 요청")
public class EmailVerificationConfirmDTO {

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Schema(description = "인증번호를 받은 이메일 주소", example = "user@example.com")
    private String email;

    @NotBlank(message = "인증번호는 필수 입력값입니다.")
    @Pattern(regexp = "^[0-9]{6}$", message = "인증번호는 6자리 숫자입니다.")
    @Schema(description = "6자리 인증번호", example = "123456")
    private String code;
}