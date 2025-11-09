package com.groomeasy.backend.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ResultCode{
	//global
	SUCCESS(HttpStatus.OK, 0, "정상 처리 되었습니다.");

	private final HttpStatus status;
	private final int code;
	private final String message;
}
