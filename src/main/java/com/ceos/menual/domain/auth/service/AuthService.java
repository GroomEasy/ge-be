package com.ceos.menual.domain.auth.service;

import java.time.Duration;
import java.util.UUID;

import com.ceos.menual.domain.auth.api.KakaoOauthClient;
import com.ceos.menual.domain.auth.dto.request.LoginRequestDTO;
import com.ceos.menual.domain.auth.dto.response.KakaoUserResponseDTO;
import com.ceos.menual.domain.auth.dto.response.LoginResponseDTO;
import com.ceos.menual.domain.auth.exception.AuthErrorCode;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.User;
import com.ceos.menual.entity.enums.AuthProvider;
import com.ceos.menual.entity.enums.UserType;
import com.ceos.menual.global.config.jwt.CookieUtil;
import com.ceos.menual.global.config.jwt.JwtProvider;
import com.ceos.menual.global.config.jwt.JwtValidator;
import com.ceos.menual.global.config.redis.RedisRefreshTokenStore;
import com.ceos.menual.global.config.redis.RefreshTokenStore;
import com.ceos.menual.global.exception.GlobalException;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final JwtValidator jwtValidator;
    private final CookieUtil cookieUtil;
    private final KakaoOauthClient kakaoOauthClient;
    private final RefreshTokenStore refreshTokenStore; // Redis

    @Value("${jwt.refresh-token-validity}")
    private long refreshTtlMillis;

    public LoginResponseDTO login(LoginRequestDTO request) {
        // 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new GlobalException(UserErrorCode.INVALID_EMAIL));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new GlobalException(UserErrorCode.INVALID_PASSWORD);
        }

        // 토큰 생성
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getUserType().name());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        // Redis에 RefreshToken 저장
        refreshTokenStore.save(user.getId(), refreshToken, Duration.ofMillis(refreshTtlMillis));

        return LoginResponseDTO.builder()
                .nickname(user.getNickname())
                .userType(user.getUserType())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public User socialLogin(String code, String provider, HttpServletResponse response) {

        Long providerId;
        String nickname;
        String email;

        if(provider.equalsIgnoreCase("KAKAO")){
            // 인가코드로 카카오 access token 발급
            String accessToken = kakaoOauthClient.getAccessToken(code);

            // accessToken으로 사용자 정보 요청
            KakaoUserResponseDTO kakaoUser = kakaoOauthClient.getUserInfo(accessToken);
            providerId = kakaoUser.getId();
            nickname = "kakao_" + UUID.randomUUID().toString().substring(0, 10);

            email = "kakao_" + UUID.randomUUID().toString().substring(0, 10) + "@kakao.user";
        }
        //else if(provider.equalsIgnoreCase("GOOGLE")){
        //
        // }
        else{
            throw new GlobalException(AuthErrorCode.INVALID_PROVIDER);
        }

        // DB 저장 또는 조회
        User user = userRepository.findByProviderAndProviderId(AuthProvider.valueOf(provider.toUpperCase()), providerId.toString())
            .orElseGet(() -> userRepository.save(
                User.builder()
                    .email(email)
                    .nickname(nickname)
                    .password("")
                    .userType(UserType.TMP_USER)
                    .provider(AuthProvider.valueOf(provider.toUpperCase()))
                    .providerId(providerId.toString())
                    .agreeTerms(false)
                    .agreePrivacy(false)
                    .build()
            ));

        String jwtAccessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getUserType().name());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        // Redis에 RefreshToken 저장
        refreshTokenStore.save(user.getId(), refreshToken, Duration.ofMillis(refreshTtlMillis));

        cookieUtil.addAccessTokenCookie(response, jwtAccessToken);
        cookieUtil.addRefreshTokenCookie(response, refreshToken);

        return user;
    }

    /**
     * Logout 시 refreshToken Redis에서 제거 후 Cookie 삭제
     */
    @Transactional
    public void logout(Long userId, HttpServletResponse response) {
        // Redis에서 refresh 제거
        try {
            refreshTokenStore.delete(userId);
        } catch (Exception e) {
            log.warn("Redis에서 RefreshToken 삭제 실패. userId={}", userId, e);
        }

        // 쿠키 만료
        cookieUtil.deleteAccessTokenCookie(response);
        cookieUtil.deleteRefreshTokenCookie(response);

    }


    public String refresh(String refreshToken) {
        // JWT 검증
        if (refreshToken == null || !jwtValidator.validateToken(refreshToken)) {
            throw new GlobalException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 토큰 타입 확인
        String tokenType = jwtValidator.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new GlobalException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 사용자 정보 조회
        Long userId = jwtValidator.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.INVALID_EMAIL));

        // Redis에 저장된 토큰과 비교
        String storedToken = refreshTokenStore.get(userId);
        if (storedToken != null && !storedToken.equals(refreshToken)) {
            throw new GlobalException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 새로운 Access Token 생성
        return jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getUserType().name());
    }
}