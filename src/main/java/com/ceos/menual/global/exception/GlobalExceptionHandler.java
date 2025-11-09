package com.ceos.menual.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ceos.menual.domain.common.dto.response.CommonResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
	//GlobalException 발생 시 반환 형태
	@ExceptionHandler(GlobalException.class)
	public ResponseEntity<CommonResponse<Void>> handleException(GlobalException e){
		return ResponseEntity.status(e.getResultCode().getStatus())
			.body(new CommonResponse<>(e.getResultCode()));
	}
}
