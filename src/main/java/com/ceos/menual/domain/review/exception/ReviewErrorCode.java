package com.ceos.menual.domain.review.exception;

import com.ceos.menual.global.exception.ResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ResultCode {

    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, 5101, "후기를 찾을 수 없습니다."),
    CONSULTATION_NOT_FOUND(HttpStatus.NOT_FOUND, 5102, "상담을 찾을 수 없습니다."),
    UNAUTHORIZED_REVIEW_ACCESS(HttpStatus.FORBIDDEN, 5103, "해당 후기에 접근할 권한이 없습니다."),
    REVIEW_ALREADY_WRITTEN(HttpStatus.BAD_REQUEST, 5104, "이미 후기가 작성된 상담입니다."),
    CONSULTATION_NOT_COMPLETED(HttpStatus.BAD_REQUEST, 5105, "완료되지 않은 상담에는 후기를 작성할 수 없습니다."),
    INVALID_RATING(HttpStatus.BAD_REQUEST, 5106, "평점은 1~5 사이여야 합니다."),
    TOO_MANY_HASHTAGS(HttpStatus.BAD_REQUEST, 5107, "해시태그는 최대 5개까지 입력 가능합니다."),
    HASHTAG_TOO_LONG(HttpStatus.BAD_REQUEST, 5108, "해시태그는 20자를 초과할 수 없습니다."),
    EMPTY_HASHTAG(HttpStatus.BAD_REQUEST, 5109, "빈 해시태그는 입력할 수 없습니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}