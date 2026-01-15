package com.ceos.menual.domain.expert.exception;

import com.ceos.menual.global.exception.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ZoomErrorCode implements ResultCode {

    ZOOM_OAUTH_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, 7001, "Zoom OAuth 설정이 누락되었습니다."),
    ZOOM_OAUTH_STATE_INVALID(HttpStatus.BAD_REQUEST, 7002, "유효하지 않은 OAuth state 입니다."),
    ZOOM_OAUTH_TOKEN_EXCHANGE_FAILED(HttpStatus.BAD_GATEWAY, 7003, "Zoom 토큰 교환에 실패했습니다."),
    ZOOM_OAUTH_USERINFO_FAILED(HttpStatus.BAD_GATEWAY, 7004, "Zoom 사용자 정보 조회에 실패했습니다."),
    ZOOM_TOKEN_REFRESH_FAILED(HttpStatus.BAD_GATEWAY, 7005, "Zoom 토큰 갱신에 실패했습니다."),
    ZOOM_MEETING_CREATE_FAILED(HttpStatus.BAD_GATEWAY, 7006, "Zoom 미팅 생성에 실패했습니다."),
    ZOOM_NOT_CONNECTED(HttpStatus.BAD_REQUEST, 7007, "전문가 Zoom 계정이 연동되어 있지 않습니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}

