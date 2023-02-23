package com.example.template.global.response;

import com.example.template.global.code.ErrorCode;
import com.example.template.global.exception.BaseException;

public class ApiResponse<T> {

	private boolean success;
	private T response;
	private ApiError error;

	private ApiResponse(boolean success, T response, ApiError error) {
		this.success = success;
		this.response = response;
		this.error = error;
	}

	static class ApiError {

		private String code;

		private String message;

		public <E extends Enum<E> & ErrorCode> ApiError(E e) {
			this.code = e.getCode();
			this.message = e.getMessage();
		}
		
		public <E extends ErrorCode> ApiError(E e) {
			this.code = e.getCode();
			this.message = e.getMessage();
		}

		public ApiError(BaseException e) {
			this.code = e.getCode();
			this.message = e.getMessage();
		}

		public String getCode() {
			return code;
		}

		public String getMessage() {
			return message;
		}

	}

	public static <T> ApiResponse<T> createSuccess(T data) {
		return new ApiResponse<>(true, data, null);
	}

	public static <T> ApiResponse<T> createSuccess() {
		return new ApiResponse<>(true, null, null);
	}
	
	public static <T, E extends ErrorCode> ApiResponse<T> createFail(E e) {
		return new ApiResponse<>(false, null, new ApiError(e));
	}

	public static <T, E extends Enum<E> & ErrorCode> ApiResponse<T> createFail(E e) {
		return new ApiResponse<>(false, null, new ApiError(e));
	}

	public static <T> ApiResponse<T> createFail(BaseException e) {
		return new ApiResponse<>(false, null, new ApiError(e));
	}

	public boolean isSuccess() {
		return success;
	}

	public T getResponse() {
		return response;
	}

	public ApiError getError() {
		return error;
	}

}
