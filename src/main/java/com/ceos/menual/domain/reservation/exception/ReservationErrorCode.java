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
    GENERAL_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, 4008, "일반 회원 프로필이 존재하지 않습니다."),
    CONSULTATION_TYPE_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, 4009, "해당 전문가가 제공하지 않는 상담 유형입니다."),
    INVALID_PRICE(HttpStatus.BAD_REQUEST, 4010, "잘못된 가격입니다."),
    UNAUTHORIZED_RESERVATION_ACCESS(HttpStatus.FORBIDDEN, 4011, "해당 예약에 대한 접근 권한이 없습니다."),
    CONCERN_JSON_CONVERSION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 4012, "고민지 JSON 변환 중 오류가 발생했습니다."),
    INVALID_RESERVATION_STATUS(HttpStatus.BAD_REQUEST, 4013, "유효하지 않은 예약 상태입니다."),
    INVALID_IMAGE_KEY_FORMAT(HttpStatus.BAD_REQUEST, 4014, "잘못된 이미지 키 형식입니다. 형식: tmp/consultation/reservation-{reservationId}/{imageType}/{fileName}"),
    MISSING_FASHION_CONCERN_DATA(HttpStatus.BAD_REQUEST, 4015, "패션 상담 고민지 데이터가 필요합니다."),
    MISSING_HAIR_CONCERN_DATA(HttpStatus.BAD_REQUEST, 4016, "헤어 상담 고민지 데이터가 필요합니다."),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, 4017, "예약 카테고리와 요청 카테고리가 일치하지 않습니다."),
    INVALID_CONSULTATION_TYPE(HttpStatus.BAD_REQUEST, 4018, "유효하지 않은 상담 유형입니다."),
    CONSULTATION_NOT_LINKED(HttpStatus.NOT_FOUND, 4019, "예약과 연결된 상담이 존재하지 않습니다."),
    INVALID_POINTS_AMOUNT(HttpStatus.BAD_REQUEST, 4020, "포인트는 0 이상이어야 합니다."),
    INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, 4021, "사용 가능한 포인트가 부족합니다."),
    MISSING_CONCERN_DATA(HttpStatus.BAD_REQUEST, 4022, "고민지 작성이 필요합니다."),
    EXPERT_CANNOT_RESERVE(HttpStatus.FORBIDDEN, 4023, "전문가는 상담을 예약할 수 없습니다.");


    private final HttpStatus status;
    private final int code;
    private final String message;
}