package com.ceos.menual.domain.chat.exception;

import com.ceos.menual.global.exception.ResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode  implements ResultCode {

    ERROR_SAVING_MESSAGE(HttpStatus.INTERNAL_SERVER_ERROR, 5001, "메시지 저장 중 오류가 발생했습니다."),
    CHATROOM_NOT_FOUND(HttpStatus.NOT_FOUND, 5002, "채팅방을 찾을 수 없습니다."),
    CHATROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, 5003, "채팅방에 접근 권한이 없습니다."),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, 5004, "메시지를 찾을 수 없습니다.");


    private final HttpStatus status;
    private final int code;
    private final String message;
}