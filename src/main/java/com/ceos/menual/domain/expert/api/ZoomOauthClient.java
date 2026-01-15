package com.ceos.menual.domain.expert.api;

import com.ceos.menual.domain.expert.dto.response.ZoomTokenResponseDTO;
import com.ceos.menual.domain.expert.dto.response.ZoomUserInfoResponseDTO;
import com.ceos.menual.domain.expert.exception.ZoomErrorCode;
import com.ceos.menual.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class ZoomOauthClient {

    private final WebClient zoomAuthWebClient;
    private final WebClient zoomApiWebClient;

    public ZoomTokenResponseDTO exchangeCodeForToken(
        String clientId,
        String clientSecret,
        String redirectUri,
        String code
    ) {
        try {
            String basic = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8)
            );

            ZoomTokenResponseDTO token = zoomAuthWebClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/oauth/token")
                    .queryParam("grant_type", "authorization_code")
                    .queryParam("code", code)
                    .queryParam("redirect_uri", redirectUri)
                    .build())
                .header("Authorization", "Basic " + basic)
                .retrieve()
                .bodyToMono(ZoomTokenResponseDTO.class)
                .block();

            if (token == null || token.getAccessToken() == null) {
                throw new GlobalException(ZoomErrorCode.ZOOM_OAUTH_TOKEN_EXCHANGE_FAILED);
            }
            return token;
        } catch (WebClientRequestException e) {
            throw new GlobalException(ZoomErrorCode.ZOOM_OAUTH_TOKEN_EXCHANGE_FAILED);
        }
    }

    public ZoomTokenResponseDTO refreshAccessToken(
        String clientId,
        String clientSecret,
        String refreshToken
    ) {
        try {
            String basic = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8)
            );

            ZoomTokenResponseDTO token = zoomAuthWebClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/oauth/token")
                    .queryParam("grant_type", "refresh_token")
                    .queryParam("refresh_token", refreshToken)
                    .build())
                .header("Authorization", "Basic " + basic)
                .retrieve()
                .bodyToMono(ZoomTokenResponseDTO.class)
                .block();

            if (token == null || token.getAccessToken() == null) {
                throw new GlobalException(ZoomErrorCode.ZOOM_TOKEN_REFRESH_FAILED);
            }
            return token;
        } catch (WebClientRequestException e) {
            throw new GlobalException(ZoomErrorCode.ZOOM_TOKEN_REFRESH_FAILED);
        }
    }

    public ZoomUserInfoResponseDTO getUserInfo(String accessToken) {
        try {
            ZoomUserInfoResponseDTO userInfo = zoomApiWebClient.get()
                .uri("/v2/users/me")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(ZoomUserInfoResponseDTO.class)
                .block();

            if (userInfo == null || userInfo.getId() == null) {
                throw new GlobalException(ZoomErrorCode.ZOOM_OAUTH_USERINFO_FAILED);
            }
            return userInfo;
        } catch (WebClientRequestException e) {
            throw new GlobalException(ZoomErrorCode.ZOOM_OAUTH_USERINFO_FAILED);
        }
    }
}

