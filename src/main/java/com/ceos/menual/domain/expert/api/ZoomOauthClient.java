package com.ceos.menual.domain.expert.api;

import com.ceos.menual.domain.expert.dto.response.ZoomTokenResponseDTO;
import com.ceos.menual.domain.expert.dto.response.ZoomUserInfoResponseDTO;
import com.ceos.menual.domain.expert.exception.ZoomErrorCode;
import com.ceos.menual.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
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
            ZoomTokenResponseDTO token = zoomAuthWebClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/oauth/token")
                    .queryParam("grant_type", "authorization_code")
                    .queryParam("code", code)
                    .queryParam("redirect_uri", redirectUri)
                    .build())
                .header("Authorization", buildBasicAuthHeader(clientId, clientSecret))
                .retrieve()
                .bodyToMono(ZoomTokenResponseDTO.class)
                .block();

            if (token == null || token.getAccessToken() == null) {
                throw new GlobalException(ZoomErrorCode.ZOOM_OAUTH_TOKEN_EXCHANGE_FAILED);
            }
            return token;
        } catch (WebClientResponseException e) {
            log.warn(
                "Zoom OAuth token exchange failed. status={}, body={}",
                e.getStatusCode(),
                e.getResponseBodyAsString()
            );
            throw new GlobalException(ZoomErrorCode.ZOOM_OAUTH_TOKEN_EXCHANGE_FAILED);
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
            ZoomTokenResponseDTO token = zoomAuthWebClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/oauth/token")
                    .queryParam("grant_type", "refresh_token")
                    .queryParam("refresh_token", refreshToken)
                    .build())
                .header("Authorization", buildBasicAuthHeader(clientId, clientSecret))
                .retrieve()
                .bodyToMono(ZoomTokenResponseDTO.class)
                .block();

            if (token == null || token.getAccessToken() == null) {
                throw new GlobalException(ZoomErrorCode.ZOOM_TOKEN_REFRESH_FAILED);
            }
            return token;
        } catch (WebClientResponseException e) {
            log.warn(
                "Zoom OAuth refresh token failed. status={}, body={}",
                e.getStatusCode(),
                e.getResponseBodyAsString()
            );
            throw new GlobalException(ZoomErrorCode.ZOOM_TOKEN_REFRESH_FAILED);
        } catch (WebClientRequestException e) {
            throw new GlobalException(ZoomErrorCode.ZOOM_TOKEN_REFRESH_FAILED);
        }
    }

    private String buildBasicAuthHeader(String clientId, String clientSecret) {
        String encoded = Base64.getEncoder().encodeToString(
            (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8)
        );
        return "Basic " + encoded;
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
        } catch (WebClientResponseException e) {
            log.warn(
                "Zoom OAuth userinfo failed. status={}, body={}",
                e.getStatusCode(),
                e.getResponseBodyAsString()
            );
            throw new GlobalException(ZoomErrorCode.ZOOM_OAUTH_USERINFO_FAILED);
        } catch (WebClientRequestException e) {
            throw new GlobalException(ZoomErrorCode.ZOOM_OAUTH_USERINFO_FAILED);
        }
    }
}

