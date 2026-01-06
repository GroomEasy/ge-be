package com.ceos.menual.global.config.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final JwtValidator jwtValidator;

    //
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {

        logRequestBasics(request); // 변경

        // 클라이언트에서 accessToken 쿠키 추출
        String accessToken = getTokenFromCookie(request, "accessToken");

        log.info("[JwtAuth] accessToken from cookie: {}", // 변경
                (accessToken == null ? "MISSING" : "FOUND(len=" + accessToken.length() + ", " + mask(accessToken) + ")")); // 변경

        // Swagger 테스트용 Bearer Token 지원
        if (accessToken == null) {
            String header = request.getHeader("Authorization");

            boolean hasBearer = (header != null && header.startsWith("Bearer ")); // 변경
            log.info("[JwtAuth] Authorization header: {}", (hasBearer ? "FOUND(Bearer)" : "MISSING/NOT_BEARER")); // 변경

            if (hasBearer) { // 변경
                accessToken = header.substring(7);
                log.info("[JwtAuth] accessToken from header: FOUND(len={}, {})", accessToken.length(), mask(accessToken)); // 변경
            }
        }

        if (accessToken != null) {
            try {
                // accessToken의 유효성 및 서명 검증
                boolean valid = jwtValidator.validateToken(accessToken); // 변경
                log.info("[JwtAuth] validateToken: {}", valid ? "PASS" : "FAIL"); // 변경

                if (valid) { // 변경
                    // 토큰에 저장된 토큰 type 확인
                    String tokenType = jwtValidator.getTokenType(accessToken);
                    log.info("[JwtAuth] tokenType: {}", tokenType); // 변경

                    // access 토큰인 경우에만 인증 처리
                    if ("access".equals(tokenType)) {
                        Long userId = jwtValidator.getUserIdFromToken(accessToken);
                        log.info("[JwtAuth] userId extracted: {}", userId); // 변경

                        // 인증 객체를 생성해 SecurityContext에 저장
                        // 이후 컨트롤러에서 @AuthenticationPrincipal 등으로 접근 가능
                        setAuthentication(request, userId);

                        log.info("[JwtAuth] SecurityContext authentication set (ROLE_USER)"); // 변경
                    } else {
                        log.warn("[JwtAuth] tokenType is not 'access' -> skip authentication"); // 변경
                    }
                }
            } catch (Exception e) {
                log.error("JWT 인증 실패: {}", e.getMessage());
                request.setAttribute("exception", e);
            }
        } else {
            log.warn("[JwtAuth] No token provided (cookie+header both missing) -> request will be unauthenticated"); // 변경
        }

        // 다음 필터로 전달
        filterChain.doFilter(request, response);
    }

    // SecurityContext에 인증 정보를 등록하는 메서드
    private void setAuthentication(HttpServletRequest request, Long userId) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // Cookie에서 Token을 꺼내는 메서드
    private String getTokenFromCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void logRequestBasics(HttpServletRequest request) { // 변경
        String origin = request.getHeader("Origin"); // 변경
        String referer = request.getHeader("Referer"); // 변경
        String ua = Optional.ofNullable(request.getHeader("User-Agent")).orElse("-"); // 변경
        String host = request.getHeader("Host"); // 변경
        String proto = request.getHeader("X-Forwarded-Proto"); // 변경
        String xfHost = request.getHeader("X-Forwarded-Host"); // 변경

        Cookie[] cookies = request.getCookies(); // 변경
        String cookieNames = (cookies == null) ? "(none)" // 변경
                : Arrays.stream(cookies).map(Cookie::getName).collect(Collectors.joining(",")); // 변경

        log.info("[JwtAuth] {} {} host={} origin={} referer={} xfProto={} xfHost={} ua={} cookieNames={}", // 변경
                request.getMethod(), // 변경
                request.getRequestURI(), // 변경
                host, // 변경
                origin, // 변경
                referer, // 변경
                proto, // 변경
                xfHost, // 변경
                shorten(ua, 80), // 변경
                cookieNames // 변경
        );
    }

    private String mask(String token) { // 변경
        if (token == null) return "-"; // 변경
        int n = token.length(); // 변경
        if (n <= 10) return "prefix=" + token.charAt(0) + "***"; // 변경
        return "prefix=" + token.substring(0, 6) + "***"; // 변경
    }

    private String shorten(String s, int max) { // 변경
        if (s == null) return "-"; // 변경
        return s.length() <= max ? s : s.substring(0, max) + "..."; // 변경
    }
}