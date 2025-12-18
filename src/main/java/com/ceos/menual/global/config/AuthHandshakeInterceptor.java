package com.ceos.menual.global.config;

import com.ceos.menual.global.config.jwt.JwtProvider;
import com.ceos.menual.global.config.jwt.JwtValidator;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtProvider jwtProvider;
    private final JwtValidator jwtValidator;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpRequest = servletRequest.getServletRequest();

            // 쿠키에서 토큰 추출
            Cookie[] cookies = httpRequest.getCookies();
            String token = null;

            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("accessToken".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }

            // 토큰 유효성 검증
            if (token != null && jwtValidator.validateToken(token)) {
                log.info("Handshake: 토큰 검증 성공");

                // STOMP 세션에서 사용할 수 있도록 userId 저장
                Long userId = jwtProvider.getUserId(token);
                attributes.put("userId", userId);

                return true;
            }

            log.warn("Handshake: 유효한 토큰 쿠키 없음 또는 검증 실패");
        }

        return false;   // 검증 실패 시 연결 거부
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }
}