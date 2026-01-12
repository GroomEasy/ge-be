package com.ceos.menual.domain.consultation.exception;

import com.ceos.menual.global.exception.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ConsultationErrorCode  implements ResultCode {

    CONSULTATION_NOT_FOUND(HttpStatus.NOT_FOUND, 3001, "존재하지 않는 상담입니다."),
    CONSULTATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, 3002, "해당 상담에 대한 접근 권한이 없습니다."),
    UNAUTHORIZED_CONSULTATION(HttpStatus.FORBIDDEN, 3003, "이 작업을 수행할 권한이 없습니다."),
    EXPERT_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, 3004, "전문가 프로필이 없습니다."),
    INVALID_USER_ROLE(HttpStatus.FORBIDDEN, 3005, "유효하지 않은 사용자 역할입니다."),
    CONSULTATION_EXPERT_PROFILE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, 3006, "상담과 연결된 전문가 프로필이 없습니다. (데이터 무결성 오류)");

    private final HttpStatus status;
    private final int code;
    private final String message;
}