package com.ceos.menual.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GlobalException extends RuntimeException {
	private final ResultCode resultCode;
}
