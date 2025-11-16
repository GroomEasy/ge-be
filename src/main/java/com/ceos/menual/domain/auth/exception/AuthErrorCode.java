package com.ceos.menual.domain.auth.exception;

import com.ceos.menual.global.exception.ResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode  implements ResultCode {

    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, 1001, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, 1002, "만료된 토큰입니다."),
    UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, 1003, "지원되지 않는 토큰입니다."),
    MALFORMED_TOKEN(HttpStatus.UNAUTHORIZED, 1004, "잘못된 형식의 토큰입니다."),
    INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, 1005, "잘못된 JWT 서명입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, 1006, "Refresh Token을 찾을 수 없습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, 1007, "유효하지 않은 Refresh Token입니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, 1008, "토큰이 존재하지 않습니다."),
    INVALID_PROVIDER(HttpStatus.BAD_REQUEST, 1009, "지원하지 않는 소셜 제공자입니다."),
    KAKAO_TOKEN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 1010, "카카오 토큰 요청에 실패했습니다."),
    KAKAO_USER_INFO_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 1011, "카카오 사용자 정보 요청에 실패했습니다.")
    ;

    private final HttpStatus status;
    private final int code;
    private final String message;
}