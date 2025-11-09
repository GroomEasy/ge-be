package com.ceos.menual.domain.auth.dto.response;

import com.ceos.menual.entity.enums.UserType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    private String nickname;

    private UserType userType;

    // 토큰은 쿠키로 전송되므로 응답 body에서 제외
    @JsonIgnore
    private String accessToken;

    @JsonIgnore
    private String refreshToken;
}