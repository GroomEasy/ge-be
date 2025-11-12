package com.ceos.menual.domain.auth.dto.response;

import com.ceos.menual.entity.enums.UserType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "로그인 응답 DTO")
public class LoginResponseDTO {

    @Schema(description = "사용자 닉네임")
    private String nickname;

    @Schema(description = "사용자 유형 (MENUAL/EXPERT)")
    private UserType userType;

    // 토큰은 쿠키로 전송되므로 응답 body에서 제외
    @JsonIgnore
    private String accessToken;

    @JsonIgnore
    private String refreshToken;
}