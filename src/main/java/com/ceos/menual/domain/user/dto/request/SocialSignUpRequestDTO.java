package com.ceos.menual.domain.user.dto.request;

import com.ceos.menual.entity.enums.UserType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "소셜 사용자 회원가입 요청 DTO")
public class SocialSignUpRequestDTO {

	@Schema(description = "닉네임", example = "철수")
	@NotBlank(message = "닉네임을 입력해주세요.")
	private String nickname;

	@Schema(description = "생년월일", example = "20000101")
	@NotBlank(message = "생년월일을 입력해주세요.")
	private String birth;

	@Schema(description = "이메일", example = "menual@gmail.com")
	@NotBlank(message = "이메일을 입력해주세요.")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	private String email;

	@Schema(description = "이용약관 동의", example = "true")
	@NotNull(message = "이용약관 동의는 필수입니다.")
	@AssertTrue(message = "이용약관에 동의해야 합니다.")
	private Boolean agreeTerms;

	@Schema(description = "개인정보 처리방침 동의", example = "true")
	@NotNull(message = "개인정보 처리방침 동의는 필수입니다.")
	@AssertTrue(message = "개인정보 처리방침에 동의해야 합니다.")
	private Boolean agreePrivacy;
}
