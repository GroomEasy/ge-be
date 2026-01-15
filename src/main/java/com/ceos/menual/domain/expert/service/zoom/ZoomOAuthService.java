package com.ceos.menual.domain.expert.service.zoom;

import com.ceos.menual.domain.expert.api.ZoomOauthClient;
import com.ceos.menual.domain.expert.dto.response.ZoomTokenResponseDTO;
import com.ceos.menual.domain.expert.dto.response.ZoomUserInfoResponseDTO;
import com.ceos.menual.domain.expert.exception.ZoomErrorCode;
import com.ceos.menual.domain.reservation.exception.ReservationErrorCode;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.global.exception.GlobalException;
import com.ceos.menual.entity.User;
import com.ceos.menual.entity.enums.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ZoomOAuthService {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ZoomProperties zoomProperties;
    private final ZoomOAuthStateStore stateStore;
    private final ZoomOauthClient zoomOauthClient;
    private final UserRepository userRepository;

    public URI buildAuthorizeRedirectUrl(Long expertUserId) {
        validateConfig();

        String state = generateState();
        stateStore.save(state, expertUserId, STATE_TTL);

        // scope는 공백으로 구분되는 경우가 많아(URL에선 %20로 인코딩 필요)
        // build(true)는 "이미 인코딩된 값"으로 간주하여 공백을 허용하지 않으므로 encode()를 사용
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromHttpUrl("https://zoom.us/oauth/authorize")
            .queryParam("response_type", "code")
            .queryParam("client_id", zoomProperties.getClientId())
            .queryParam("redirect_uri", zoomProperties.getRedirectUri())
            .queryParam("state", state);

        if (!isBlank(zoomProperties.getScope())) {
            builder.queryParam("scope", zoomProperties.getScope());
        }

        return builder.build().encode().toUri();
    }

    /**
     * code/state로 토큰 교환 후 /users/me 호출까지 수행
     * ExpertProfile에 Zoom 연동 정보를 저장
     */
    @Transactional
    public ZoomOAuthCallbackResult handleCallback(String code, String state) {
        validateConfig();

        Long expertUserId = stateStore.consume(state)
            .orElseThrow(() -> new GlobalException(ZoomErrorCode.ZOOM_OAUTH_STATE_INVALID));

        log.info("Zoom OAuth callback received - expertUserId={}", expertUserId);

        ZoomTokenResponseDTO token = zoomOauthClient.exchangeCodeForToken(
            zoomProperties.getClientId(),
            zoomProperties.getClientSecret(),
            zoomProperties.getRedirectUri(),
            code
        );

        ZoomUserInfoResponseDTO me = zoomOauthClient.getUserInfo(token.getAccessToken());

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(
            token.getExpiresIn() != null ? token.getExpiresIn() : 3600
        );

        // ExpertProfile에 Zoom 연동 정보 저장
        User expertUser = userRepository.findById(expertUserId)
            .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        if (expertUser.getUserType() != UserType.EXPERT || expertUser.getExpertProfile() == null) {
            throw new GlobalException(ReservationErrorCode.EXPERT_PROFILE_NOT_FOUND);
        }

        expertUser.getExpertProfile().connectZoom(
            me.getId(),
            token.getAccessToken(),
            token.getRefreshToken(),
            expiresAt
        );

        // dirty checking 보장 + 디버깅 편의용
        userRepository.saveAndFlush(expertUser);
        log.info("Zoom connected and saved - expertUserId={}, zoomUserId={}, expiresAt={}",
            expertUserId, me.getId(), expiresAt);

        return ZoomOAuthCallbackResult.builder()
            .expertUserId(expertUserId)
            .zoomUserId(me.getId())
            .zoomEmail(me.getEmail())
            .tokenExpiresAt(expiresAt)
            .build();
    }

    private void validateConfig() {
        if (isBlank(zoomProperties.getClientId())
            || isBlank(zoomProperties.getClientSecret())
            || isBlank(zoomProperties.getRedirectUri())) {
            throw new GlobalException(ZoomErrorCode.ZOOM_OAUTH_NOT_CONFIGURED);
        }
    }

    private String generateState() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

