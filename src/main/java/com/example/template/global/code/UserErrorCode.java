package com.example.template.global.code;

public enum UserErrorCode implements ErrorCode {

	NOT_FOUND("U001", "존재하지 않는 사용자입니다.");

	private String code;
	private String message;

	private UserErrorCode(String code, String message) {
		this.code = code;
		this.message = message;
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return message;
	}
}