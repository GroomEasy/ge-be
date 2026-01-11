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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final JwtValidator jwtValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {

        // 클라이언트에서 accessToken 쿠키 추출
        String accessToken = getTokenFromCookie(request, "accessToken");
        log.debug("쿠키에서 토큰 찾음: {}", accessToken != null ? "있음" : "없음");

        // Swagger 테스트용 Bearer Token 지원
        if (accessToken == null) {
            String header = request.getHeader("Authorization");
            log.debug("Authorization 헤더: {}", header);
            if (header != null && header.startsWith("Bearer ")) {
                accessToken = header.substring(7);
                log.debug("Authorization 헤더에서 Bearer 토큰 추출됨");
            }
        }

        if (accessToken != null) {
            try {
                log.debug("토큰 검증 시작");
                // accessToken의 유효성 및 서명 검증
                if (jwtValidator.validateToken(accessToken)) {
                    log.debug("토큰 검증 성공");
                    // 토큰에 저장된 토큰 type 확인
                    String tokenType = jwtValidator.getTokenType(accessToken);
                    log.debug("토큰 타입: {}", tokenType);
                    // access 토큰인 경우에만 인증 처리
                    if ("access".equals(tokenType)) {
                        Long userId = jwtValidator.getUserIdFromToken(accessToken);
                        String userType = jwtValidator.getUserTypeFromToken(accessToken);
                        log.debug("사용자 ID: {}, 사용자 타입: {}", userId, userType);
                        // 인증 객체를 생성해 SecurityContext에 저장
                        // 이후 컨트롤러에서 @AuthenticationPrincipal 등으로 접근 가능
                        setAuthentication(request, userId, userType);
                        log.debug("인증 설정 완료 - 역할: {}", userType);
                    } else {
                        log.warn("토큰 타입이 access가 아님: {}", tokenType);
                    }
                } else {
                    log.warn("토큰 검증 실패");
                }
            } catch (Exception e) {
                log.error("JWT 인증 실패: {}", e.getMessage(), e);
                request.setAttribute("exception", e);
            }
        } else {
            log.debug("토큰을 찾을 수 없음");
        }

        // 다음 필터로 전달
        filterChain.doFilter(request, response);
    }

    // SecurityContext에 인증 정보를 등록하는 메서드
    private void setAuthentication(HttpServletRequest request, Long userId, String userType) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        // 기본 권한
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        // userType에 따라 추가 권한 부여
        if ("ADMIN".equals(userType)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            log.debug("ROLE_ADMIN 권한 추가됨");
        } else if ("EXPERT".equals(userType)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_EXPERT"));
            log.debug("ROLE_EXPERT 권한 추가됨");
        }
        
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                authorities
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

}
