package com.ceos.menual.domain.user.controller;

import com.ceos.menual.domain.common.dto.response.CommonResponse;
import com.ceos.menual.domain.user.dto.request.ExpertConversionRequestDTO;
import com.ceos.menual.domain.user.dto.request.SignUpRequestDTO;
import com.ceos.menual.domain.user.dto.request.SocialSignUpRequestDTO;
import com.ceos.menual.domain.user.dto.response.ExpertConversionResponseDTO;
import com.ceos.menual.domain.user.dto.response.SignUpResponseDTO;
import com.ceos.menual.domain.user.dto.response.SocialSignUpResponseDTO;
import com.ceos.menual.domain.user.dto.response.UserInfoResponseDTO;
import com.ceos.menual.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "회원 API", description = "회원 관련 엔드포인트")
public class UserController {

    private final UserService userService;

    /**
     * 회원가입(기본 로그인 회원) API 엔드포인트
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

    /**
     * 내 정보 조회
     */
    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인한 사용자의 정보를 반환합니다."
    )
    @GetMapping("/me")
    public ResponseEntity<CommonResponse<UserInfoResponseDTO>> getMyInfo(
            @AuthenticationPrincipal Long userId) {
        UserInfoResponseDTO response = userService.getMyInfo(userId);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 전문가 전환
     */
    @Operation(
            summary = "전문가 전환",
            description = "일반 회원이 전문가로 전환합니다. GeneralProfile이 삭제되고 ExpertProfile이 생성됩니다."
    )
    @PostMapping("/conversion")
    public ResponseEntity<CommonResponse<ExpertConversionResponseDTO>> convertToExpert(
            @Parameter(description = "사용자 ID", required = true)
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ExpertConversionRequestDTO requestDTO
    ) {
        ExpertConversionResponseDTO response = userService.convertToExpert(userId, requestDTO);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 포인트 적립 내역 조회
     */
    @Operation(
            summary = "포인트 적립 내역 조회",
            description = "사용자의 포인트 적립 및 사용 내역을 조회합니다. 현재 보유 포인트와 히스토리를 반환합니다."
    )
    @GetMapping("/points/history")
    public ResponseEntity<CommonResponse<com.ceos.menual.domain.user.dto.response.PointHistoryListResponseDTO>> getPointHistory(
            @Parameter(description = "사용자 ID", required = true)
            @AuthenticationPrincipal Long userId
    ) {
        com.ceos.menual.domain.user.dto.response.PointHistoryListResponseDTO response =
                userService.getPointHistory(userId);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
