package com.ceos.menual.domain.auth.api;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import com.ceos.menual.domain.auth.dto.response.KakaoUserResponseDTO;
import com.ceos.menual.domain.auth.exception.AuthErrorCode;
import com.ceos.menual.global.exception.GlobalException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoOauthClient {

	@Value("${kakao.client-id}")
	private String clientId;

	@Value("${kakao.redirect-uri}")
	private String redirectUri;

	private final WebClient kakaoAuthWebClient;
	private final WebClient kakaoApiWebClient;

	public String getAccessToken(String code) {
		try {
			return kakaoAuthWebClient.post()
				.uri(uriBuilder -> uriBuilder
					.path("/oauth/token")
					.queryParam("grant_type", "authorization_code")
					.queryParam("client_id", clientId)
					.queryParam("redirect_uri", redirectUri)
					.queryParam("code", code)
					.build())
				.retrieve()
				.bodyToMono(Map.class)
				.map(res -> {
					String accessToken = (String) res.get("access_token");
					if (accessToken == null) {
						throw new GlobalException(AuthErrorCode.KAKAO_TOKEN_ERROR);
					}
					return accessToken;
				})
				.block();
		} catch (WebClientRequestException e){
			throw new GlobalException(AuthErrorCode.KAKAO_TOKEN_ERROR);
		}
	}

	public KakaoUserResponseDTO getUserInfo(String accessToken){
		try {
			KakaoUserResponseDTO userInfo = kakaoApiWebClient.get()
				.uri("/v2/user/me")
				.headers(headers -> headers.setBearerAuth(accessToken))
				.retrieve()
				.bodyToMono(KakaoUserResponseDTO.class)
				.block();

			if (userInfo == null){
				throw new GlobalException(AuthErrorCode.KAKAO_USER_INFO_ERROR);
			}
			return userInfo;

		}catch (WebClientRequestException e){
			throw new GlobalException(AuthErrorCode.KAKAO_USER_INFO_ERROR);
		}
	}

}
