package com.ceos.menual.global.config.jwt;

import io.jsonwebtoken.Jwts;

import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
@Getter
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Getter
    @Value("${jwt.access-token-validity}")
    private long accessTokenValidity;

    @Getter
    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    private SecretKey key;

    public JwtProvider(@Value("${jwt.secret}") String secret) {
        // 최소 32바이트(256비트) 검증
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret key must be at least 32 bytes long (256 bits, HS256 standard)"
            );
        }
        this.key = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    // Access Token 생성
    public String createAccessToken(Long userId, String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidity);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    // Refresh Token 생성
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidity);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

}
