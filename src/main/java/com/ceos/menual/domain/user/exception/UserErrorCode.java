package com.ceos.menual.domain.auth.exception;

import com.ceos.menual.global.exception.ResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ResultCode {

    // User 관련 에러
    INVALID_EMAIL(HttpStatus.NOT_FOUND, 1101, "이메일이 일치하지 않습니다."),
    INVALID_PASSWORD(HttpStatus.CONFLICT, 1102, "비밀번호가 일치하지 않습니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}