package com.ceos.menual.domain.user.controller;

import com.ceos.menual.domain.common.dto.response.CommonResponse;
import com.ceos.menual.domain.user.dto.request.SignUpRequestDTO;
import com.ceos.menual.domain.user.dto.request.SocialSignUpRequestDTO;
import com.ceos.menual.domain.user.dto.response.SignUpResponseDTO;
import com.ceos.menual.domain.user.dto.response.SocialSignUpResponseDTO;
import com.ceos.menual.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "회원 API", description = "회원 관련 엔드포인트")
public class UserController {

    private final UserService userService;

    /*
    회원가입(기본 로그인 회원) API 엔드포인트
     */
    @Operation(
            summary = "회원가입",
            description = "사용자의 정보를 입력받아 검증한 후 DB에 저장합니다."
    )
    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<SignUpResponseDTO>> signUp(@Valid @RequestBody SignUpRequestDTO request) {
        SignUpResponseDTO response = userService.signUp(request);

        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /*
    회원가입(소셜 로그인 회원) API 엔드포인트
     */
    @Operation(
        summary = "소셜 로그인 회원가입 (추가 정보 입력)",
        description = "사용자의 정보를 입력받아 검증한 후 DB에 저장합니다."
    )
    @PostMapping("/social-signup")
    public ResponseEntity<CommonResponse<SocialSignUpResponseDTO>> socialSignUp(
        @Parameter(description = "사용자정보", required = true)
        @AuthenticationPrincipal Long userId,
        @Valid @RequestBody SocialSignUpRequestDTO request
    ) {
        SocialSignUpResponseDTO response = userService.socialSignUp(userId, request);

        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
