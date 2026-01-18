package com.ceos.menual.domain.expert.exception;

import com.ceos.menual.global.exception.ResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExpertErrorCode  implements ResultCode {

    USER_NOT_EXPERT(HttpStatus.FORBIDDEN, 6001, "전문가가 아닙니다."),
    EXPERT_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, 6002, "전문가 프로필이 존재하지 않습니다."),
    CANNOT_LIKE_SELF(HttpStatus.BAD_REQUEST, 6003, "자신을 찜할 수 없습니다."),
    ALREADY_LIKED_EXPERT(HttpStatus.CONFLICT, 6004, "이미 해당 전문가를 찜했습니다."),
    LIKE_NOT_FOUND(HttpStatus.CONFLICT, 6005, "해당 전문가에 대한 찜을 찾을 수 없습니다."),
    PORTFOLIO_NOT_FOUND(HttpStatus.NOT_FOUND, 6006, "포트폴리오를 찾을 수 없거나 본인의 포트폴리오가 아닙니다."),
    NO_REPRESENTATIVE_PORTFOLIO(HttpStatus.NOT_FOUND, 6007, "해제할 대표 포트폴리오가 없습니다."),
    NO_EXPERT_BANK_ACCOUNT(HttpStatus.NOT_FOUND, 6008, "해당 전문가의 계좌가 존재하지 않습니다."),
    INVALID_IMAGE_KEY_FORMAT(HttpStatus.BAD_REQUEST, 6009, "잘못된 이미지 키 형식입니다.");





    private final HttpStatus status;
    private final int code;
    private final String message;
}