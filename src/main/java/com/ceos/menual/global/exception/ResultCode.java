package com.ceos.menual.global.exception;

import org.springframework.http.HttpStatus;

public interface ResultCode {
	HttpStatus getStatus();
	int getCode();
	String getMessage();
}
