package com.ceos.menual.domain.expert.exception;

import com.ceos.menual.global.exception.ResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExpertErrorCode  implements ResultCode {

    USER_NOT_EXPERT(HttpStatus.FORBIDDEN, 6001, "전문가가 아닙니다.");




    private final HttpStatus status;
    private final int code;
    private final String message;
}