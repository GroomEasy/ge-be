package com.ceos.menual.domain.consultation.exception;

import com.ceos.menual.global.exception.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ConsultationErrorCode  implements ResultCode {

    CONSULTATION_NOT_FOUND(HttpStatus.NOT_FOUND, 3001, "존재하지 않는 상담입니다."),
    CONSULTATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, 3002, "해당 상담에 대한 접근 권한이 없습니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}