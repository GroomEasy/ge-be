package com.ceos.menual.global.config.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtExceptionFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            setErrorResponse(response, "만료된 토큰입니다.", HttpServletResponse.SC_UNAUTHORIZED);
        } catch (MalformedJwtException | UnsupportedJwtException e) {
            setErrorResponse(response, "유효하지 않은 토큰입니다.", HttpServletResponse.SC_UNAUTHORIZED);
        } catch (SecurityException e) {
            setErrorResponse(response, "잘못된 JWT 서명입니다.", HttpServletResponse.SC_UNAUTHORIZED);
        } catch (Exception e) {
            log.error("JWT 외 예외 발생, 필터 밖으로 전달합니다.", e);
            throw e;
        }
    }

    private void setErrorResponse(HttpServletResponse response, String message, int status) {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());

        try {
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        } catch (IOException e) {
            log.error("응답 작성 실패", e);
        }
    }
}
