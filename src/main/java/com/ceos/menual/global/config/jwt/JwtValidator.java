package com.ceos.menual.global.config.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtValidator {

    private final JwtProvider jwtProvider;

    private SecretKey getKey() {
        return jwtProvider.getKey();
    }

    // 토큰 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token);
            return true;

        } catch (ExpiredJwtException | UnsupportedJwtException |
                 MalformedJwtException | SecurityException |
                 IllegalArgumentException e) {

            log.warn("JWT 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    // 사용자 ID 추출
    public Long getUserIdFromToken(String token) {
        return Long.parseLong(
                Jwts.parser()
                        .verifyWith(getKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getSubject()
        );
    }

    // 토큰 타입 추출
    public String getTokenType(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("type", String.class);
    }

    // 사용자 타입 추출
    public String getUserTypeFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userType", String.class);
    }

}
