package com.ceos.menual.domain.auth.controller;

import com.ceos.menual.domain.auth.dto.request.KakaoLoginRequestDTO;
import com.ceos.menual.domain.auth.dto.response.KakaoLoginResponseDTO;
import com.ceos.menual.domain.auth.dto.request.LoginRequestDTO;
import com.ceos.menual.domain.auth.dto.response.LoginResponseDTO;
import com.ceos.menual.domain.auth.service.AuthService;
import com.ceos.menual.domain.common.dto.response.CommonResponse;
import com.ceos.menual.entity.User;
import com.ceos.menual.global.config.jwt.CookieUtil;

import io.swagger.v3.oas.annotations.Operation;
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

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletResponse response
    ) {
        LoginResponseDTO loginResponse = authService.login(request);

        // Access Token을 httpOnly 쿠키로 설정
        cookieUtil.addAccessTokenCookie(response, loginResponse.getAccessToken());

        // Refresh Token을 httpOnly 쿠키로 설정
        cookieUtil.addRefreshTokenCookie(response, loginResponse.getRefreshToken());

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/social-login")
    @Operation(summary = "카카오 로그인", description = "카카오 인가코드를 받아 로그인합니다.")
    public CommonResponse<KakaoLoginResponseDTO> login(@RequestBody KakaoLoginRequestDTO request, HttpServletResponse response){
        User user = authService.socialLogin(request.getCode(), request.getProvider(), response);
        KakaoLoginResponseDTO result = new KakaoLoginResponseDTO(user.getNickname(), user.getUserType().name());
        return CommonResponse.success(result);
    }


}
