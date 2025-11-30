package com.ceos.menual.domain.chat.exception;

import com.ceos.menual.global.exception.ResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode  implements ResultCode {

    ERROR_SAVING_MESSAGE(HttpStatus.BAD_REQUEST, 4001, "메시지 저장 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}