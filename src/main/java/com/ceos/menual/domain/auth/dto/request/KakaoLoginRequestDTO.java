package com.ceos.menual.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description="소셜 로그인 요청")
public class KakaoLoginRequestDTO {

	@Schema(description = "인가 코드", example="abc123xyz")
	private String code;

	@Schema(description = "소셜 제공자", example="KAKAO")
	private String provider;

}
