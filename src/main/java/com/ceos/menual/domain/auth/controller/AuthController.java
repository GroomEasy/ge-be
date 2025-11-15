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
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "인증 API", description = "인증 관련 엔드포인트")
public class AuthController {

    private final CookieUtil cookieUtil;
    private final AuthService authService;

    /*
    일반 사용자 login API 엔드포인트
    loginResponse에서 AccessToken과 RefreshToken을 꺼내 httpOnly 쿠키로 전송
     */
    @Operation(
            summary = "일반 사용자 로그인",
            description = "사용자 이메일과 비밀번호를 이용해 로그인하고, 발급된 AccessToken과 RefreshToken을 httpOnly 쿠키로 전송한다."
    )
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginResponseDTO>> socialLogin(
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

    @PostMapping("/social-login")
    @Operation(summary = "카카오 로그인", description = "카카오 인가코드를 받아 로그인합니다.")
    public CommonResponse<KakaoLoginResponseDTO> login(@RequestBody KakaoLoginRequestDTO request, HttpServletResponse response){
        User user = authService.socialLogin(request.getCode(), request.getProvider(), response);
        KakaoLoginResponseDTO result = new KakaoLoginResponseDTO(user.getNickname(), user.getUserType().name());
        return CommonResponse.success(result);
    }


}
