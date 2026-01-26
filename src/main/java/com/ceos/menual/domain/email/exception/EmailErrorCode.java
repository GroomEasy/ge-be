package com.ceos.menual.domain.email.exception;

import com.ceos.menual.global.exception.ResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EmailErrorCode implements ResultCode {

    VERIFICATION_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, 2001, "인증번호가 만료되었거나 존재하지 않습니다."),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, 2002, "인증번호가 일치하지 않습니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 2003, "이메일 전송에 실패했습니다."),
    VERIFICATION_CODE_ALREADY_SENT(HttpStatus.TOO_MANY_REQUESTS, 2004, "인증번호가 이미 발송되었습니다. 잠시 후 다시 시도해주세요."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, 2005, "이메일 인증이 완료되지 않았습니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}