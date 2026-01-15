package com.ceos.menual.domain.expert.controller;

import com.ceos.menual.domain.common.dto.response.CommonResponse;
import com.ceos.menual.domain.expert.service.zoom.ZoomOAuthCallbackResult;
import com.ceos.menual.domain.expert.service.zoom.ZoomOAuthService;
import com.ceos.menual.domain.expert.service.zoom.ZoomProperties;
import com.ceos.menual.global.exception.GlobalException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/experts/zoom")
@RequiredArgsConstructor
@Tag(name = "전문가 Zoom 연동 API", description = "전문가 Zoom OAuth 연동/콜백 처리")
public class ExpertZoomController {

    private final ZoomOAuthService zoomOAuthService;
    private final ZoomProperties zoomProperties;

    @Operation(summary = "Zoom OAuth 연동 시작", description = "Zoom 로그인/권한동의 화면으로 302 리다이렉트합니다.")
    @PreAuthorize("hasRole('EXPERT')")
    @GetMapping("/connect")
    public ResponseEntity<Void> connect(@AuthenticationPrincipal Long userId) {
        URI authorizeUrl = zoomOAuthService.buildAuthorizeRedirectUrl(userId);
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, authorizeUrl.toString())
            .build();
    }

    @Operation(
        summary = "Zoom OAuth 콜백",
        description = "Zoom에서 전달된 code/state를 처리합니다. " +
            "설정된 success/failure redirect URI가 있으면 302로 프론트로 리다이렉트하고, " +
            "없으면 기존처럼 JSON으로 결과를 반환합니다."
    )
    @GetMapping("/callback")
    public ResponseEntity<?> callback(
        @RequestParam String code,
        @RequestParam String state
    ) {
        try {
            ZoomOAuthCallbackResult result = zoomOAuthService.handleCallback(code, state);

            // 설정된 success redirect URI가 있으면 프론트로 302
            if (!isBlank(zoomProperties.getSuccessRedirectUri())) {
                URI target = UriComponentsBuilder
                    .fromUriString(zoomProperties.getSuccessRedirectUri())
                    .queryParam("connected", "true")
                    .build(true)
                    .toUri();

                return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, target.toString())
                    .build();
            }

            // 토큰 자체는 응답에 포함하지 않고, 연결 확인에 필요한 정보만 반환
            return ResponseEntity.ok(CommonResponse.success(
                new ZoomCallbackPublicResponse(
                    result.getExpertUserId(),
                    result.getZoomUserId(),
                    result.getZoomEmail(),
                    result.getTokenExpiresAt()
                )
            ));
        } catch (GlobalException e) {
            // 설정된 failure redirect URI가 있으면 프론트로 302
            if (!isBlank(zoomProperties.getFailureRedirectUri())) {
                URI target = UriComponentsBuilder
                    .fromUriString(zoomProperties.getFailureRedirectUri())
                    .queryParam("connected", "false")
                    .queryParam("error", e.getResultCode())
                    .build(true)
                    .toUri();

                return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, target.toString())
                    .build();
            }

            // 기존 동작 유지: 예외는 그대로 전파 (전역 예외 처리에서 응답 생성)
            throw e;
        }
    }

    private record ZoomCallbackPublicResponse(
        Long expertUserId,
        String zoomUserId,
        String zoomEmail,
        java.time.LocalDateTime tokenExpiresAt
    ) {}

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

