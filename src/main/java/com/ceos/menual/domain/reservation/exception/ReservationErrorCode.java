package com.ceos.menual.domain.reservation.exception;

import com.ceos.menual.global.exception.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReservationErrorCode implements ResultCode {

    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, 4001, "존재하지 않는 예약입니다."),
    RESERVATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, 4002, "해당 예약에 대한 접근 권한이 없습니다."),
    ALREADY_BOOKED_TIME(HttpStatus.CONFLICT, 4003, "해당 시간은 이미 예약되었습니다."),
    INVALID_RESERVATION_TIME(HttpStatus.BAD_REQUEST, 4004, "과거 시간은 예약할 수 없습니다."),
    MISSING_SCHEDULED_TIME(HttpStatus.BAD_REQUEST, 4005, "화상 상담은 날짜와 시간 선택이 필수입니다."),
    INVALID_MESSAGE_CONSULTATION(HttpStatus.BAD_REQUEST, 4006, "메시지 상담은 날짜와 시간을 선택할 수 없습니다."),
    EXPERT_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, 4007, "전문가 프로필이 존재하지 않습니다."),
    GENERAL_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, 4008, "일반 회원 프로필이 존재하지 않습니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}