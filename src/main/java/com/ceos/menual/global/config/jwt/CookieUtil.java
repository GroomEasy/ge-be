package com.ceos.menual.global.config.jwt;

import com.ceos.menual.domain.auth.exception.AuthErrorCode;
import com.ceos.menual.global.exception.GlobalException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieUtil {

    private final JwtProvider jwtProvider;

    private static final String ACCESS_TOKEN_NAME = "accessToken";
    private static final String REFRESH_TOKEN_NAME = "refreshToken";

    public void addAccessTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = createResponseCookie(
                ACCESS_TOKEN_NAME,
                token,
                jwtProvider.getAccessTokenValidity() / 1000,
                "/"
        );
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = createResponseCookie(
                REFRESH_TOKEN_NAME,
                token,
                jwtProvider.getRefreshTokenValidity() / 1000,
                "/api/auth/refresh"
        );
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void deleteAccessTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = createResponseCookie(ACCESS_TOKEN_NAME, "", 0, "/");
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = createResponseCookie(REFRESH_TOKEN_NAME, "", 0, "/api/auth/refresh");
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REFRESH_TOKEN_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        throw new GlobalException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    private ResponseCookie createResponseCookie(String name, String value, long maxAge, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path(path)
                .maxAge(maxAge)
                .sameSite("None")
                .build();
    }
}