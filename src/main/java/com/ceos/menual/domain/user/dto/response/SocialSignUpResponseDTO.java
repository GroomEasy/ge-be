package com.ceos.menual.domain.user.dto.response;

import com.ceos.menual.entity.User;
import com.ceos.menual.entity.enums.UserType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "소셜 사용자 회원가입 응답 DTO")
public class SocialSignUpResponseDTO {

	@Schema(description = "닉네임", example = "철수")
	private String nickname;

	@Schema(description = "회원 타입", example = "EXPERT")
	private UserType userType;

}
