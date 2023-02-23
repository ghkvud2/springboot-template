package com.example.template.global.exception;

import com.example.template.global.code.ErrorCode;

@SuppressWarnings("serial")
public abstract class BaseException extends RuntimeException {

	private String code;
	private String message;

	public BaseException(ErrorCode errorCode, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(errorCode.getMessage(), cause, enableSuppression, writableStackTrace);
		this.code = errorCode.getCode();
	}

	public BaseException(ErrorCode errorCode, Throwable cause) {
		super(errorCode.getMessage(), cause);
	}

	public BaseException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.code = errorCode.getCode();
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

}
