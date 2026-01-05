package com.ceos.menual.domain.user.exception;

import com.ceos.menual.global.exception.ResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ResultCode {

    // User 관련 에러
    INVALID_EMAIL(HttpStatus.NOT_FOUND, 1101, "이메일이 일치하지 않습니다."),
    INVALID_PASSWORD(HttpStatus.CONFLICT, 1102, "비밀번호가 일치하지 않습니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, 1103, "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, 1104, "이미 사용 중인 이메일입니다."),
    INVALID_BIRTH_FORMAT(HttpStatus.BAD_REQUEST, 1105, "올바르지 않은 생년월일 형식입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 1106, "존재하지 않는 사용자입니다."),
    GENERAL_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, 1107, "일반 회원 프로필이 존재하지 않습니다."),
    NOT_MEMBER(HttpStatus.FORBIDDEN, 1108, "일반 회원이 아닙니다.");


    private final HttpStatus status;
    private final int code;
    private final String message;
}