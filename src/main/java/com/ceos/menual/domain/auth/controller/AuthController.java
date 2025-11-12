package com.ceos.menual.domain.auth.controller;

import com.ceos.menual.domain.auth.dto.request.LoginRequestDTO;
import com.ceos.menual.domain.auth.dto.response.LoginResponseDTO;
import com.ceos.menual.domain.auth.service.AuthService;
import com.ceos.menual.domain.common.dto.response.CommonResponse;
import com.ceos.menual.global.config.jwt.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CookieUtil cookieUtil;
    private final AuthService authService;

    /*
    일반 사용자 login API 엔드포인트
    loginResponse에서 AccessToken과 RefreshToken을 꺼내 httpOnly 쿠키로 전송
     */
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletResponse response
    ) {
        LoginResponseDTO loginResponse = authService.login(request);

        // Access Token을 httpOnly 쿠키로 설정
        cookieUtil.addAccessTokenCookie(response, loginResponse.getAccessToken());

        // Refresh Token을 httpOnly 쿠키로 설정
        cookieUtil.addRefreshTokenCookie(response, loginResponse.getRefreshToken());

        return ResponseEntity.ok(CommonResponse.success(loginResponse));
    }
}
