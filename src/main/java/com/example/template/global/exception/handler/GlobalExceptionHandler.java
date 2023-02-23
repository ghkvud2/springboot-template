package com.example.template.global.exception.handler;

import javax.validation.ConstraintViolationException;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.template.global.code.ErrorCode;
import com.example.template.global.exception.BaseException;
import com.example.template.global.response.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@SuppressWarnings("rawtypes")
	@ExceptionHandler
	public ApiResponse baseExceptionHandler(BaseException e) {
		return ApiResponse.createFail(e);
	}

	@SuppressWarnings("rawtypes")
	@ExceptionHandler
	public ApiResponse constraintViolationExceptionHandler(ConstraintViolationException e) {
		String message = e.getConstraintViolations().stream().findFirst().get().getMessage();
		return ApiResponse.createFail(new ErrorWrapper(ErrorType.VALIDATION_FAIL, message));
	}

	@SuppressWarnings("rawtypes")
	@ExceptionHandler
	public ApiResponse IllegalArgumentExceptionHandler(IllegalArgumentException e) {
		return ApiResponse.createFail(new ErrorWrapper(ErrorType.ETC, e.getMessage()));
	}

	static private class ErrorWrapper implements ErrorCode {
		private ErrorType errorCode;
		private String message;

		public ErrorWrapper(ErrorType errorCode, String message) {
			this.errorCode = errorCode;
			this.message = message;
		}

		@Override
		public String getCode() {
			return errorCode.getCode();
		}

		@Override
		public String getMessage() {
			return message;
		}
	}

	static private enum ErrorType {

		ETC("9999"), VALIDATION_FAIL("8888");

		private String code;

		private ErrorType(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}
	}
}
