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
    NOT_MEMBER(HttpStatus.FORBIDDEN, 1108, "일반 회원이 아닙니다."),
    ADMIN_PERMISSION_REQUIRED(HttpStatus.FORBIDDEN, 1109, "관리자 권한이 필요합니다."),
    USER_ALREADY_EXPERT(HttpStatus.CONFLICT, 1110, "이미 전문가입니다."),
    INCOMPLETE_BANK_ACCOUNT_INFO(HttpStatus.BAD_REQUEST, 1111, "계좌 정보는 은행명, 계좌번호, 예금주명을 모두 입력해야 합니다."),
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, 1112, "사용자 프로필 정보를 찾을 수 없습니다."),
    EXPERT_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, 1113, "전문가 프로필이 존재하지 않습니다."),
    USER_ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, 1114, "이미 탈퇴한 회원입니다."),
    ACTIVE_RESERVATION_EXISTS(HttpStatus.CONFLICT, 1115, "진행 중인 예약이 있어 탈퇴할 수 없습니다."),
    ACTIVE_CONSULTATION_EXISTS(HttpStatus.CONFLICT, 1116, "진행 중인 상담이 있어 탈퇴할 수 없습니다."),
    USER_WITHDRAWN(HttpStatus.FORBIDDEN, 1117, "탈퇴한 회원은 이용할 수 없습니다.");



    private final HttpStatus status;
    private final int code;
    private final String message;
}