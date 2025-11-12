package com.ceos.menual.domain.auth.service;

import com.ceos.menual.domain.auth.dto.request.LoginRequestDTO;
import com.ceos.menual.domain.auth.dto.response.LoginResponseDTO;
import com.ceos.menual.domain.auth.exception.AuthErrorCode;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.User;
import com.ceos.menual.global.config.jwt.JwtProvider;
import com.ceos.menual.global.config.jwt.JwtValidator;
import com.ceos.menual.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final JwtValidator jwtValidator;

    public LoginResponseDTO login(LoginRequestDTO request) {
        // 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new GlobalException(UserErrorCode.INVALID_EMAIL));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new GlobalException(UserErrorCode.INVALID_PASSWORD);
        }

        // 토큰 생성
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        return LoginResponseDTO.builder()
                .nickname(user.getNickname())
                .userType(user.getUserType())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public String refresh(String refreshToken) {
        // Refresh Token 검증
        if (refreshToken == null || !jwtValidator.validateToken(refreshToken)) {
            throw new GlobalException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // Refresh Token 타입 확인
        String tokenType = jwtValidator.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new GlobalException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 사용자 정보 조회
        Long userId = jwtValidator.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.INVALID_EMAIL));

        // 새로운 Access Token 생성
        return jwtProvider.createAccessToken(user.getId(), user.getEmail());
    }
}