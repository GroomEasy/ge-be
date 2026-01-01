package com.ceos.menual.domain.expert.exception;

import com.ceos.menual.global.exception.ResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExpertErrorCode  implements ResultCode {



    private final HttpStatus status;
    private final int code;
    private final String message;
}