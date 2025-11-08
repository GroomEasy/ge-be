package com.groomeasy.backend.domain.user.dto.request;

import com.groomeasy.backend.entity.enums.UserType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class SignUpRequestDTO {

    @NotBlank(message = "닉네임을 입력해주세요.")
    private String nickname;

    @NotBlank(message = "생년월일을 입력해주세요.")
    private String birth;

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    private String passwordConfirm;

    private UserType userType;

    @NotNull(message = "이용약관 동의는 필수입니다.")
    @AssertTrue(message = "이용약관에 동의해야 합니다.")
    private Boolean agreeTerms;

    @NotNull(message = "개인정보 처리방침 동의는 필수입니다.")
    @AssertTrue(message = "개인정보 처리방침에 동의해야 합니다.")
    private Boolean agreePrivacy;

    // 비밀번호 일치 검증 메서드
    public boolean isPasswordMatch() {
        return password != null && password.equals(passwordConfirm);
    }
}
