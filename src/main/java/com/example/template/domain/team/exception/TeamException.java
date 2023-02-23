package com.example.template.domain.team.exception;

import com.example.template.global.code.ErrorCode;
import com.example.template.global.exception.BaseException;

public class TeamException {

	static enum TeamErrorCode implements ErrorCode {
		
		NOT_FOUND("T404", "존재하지 않는 팀입니다.");

		private String code;
		private String message;

		private TeamErrorCode(String code, String message) {
			this.code = code;
			this.message = message;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

	}

	public static class TeamNotFoundException extends BaseException {

		private static final long serialVersionUID = 3071276038672931799L;

		public TeamNotFoundException() {
			super(TeamErrorCode.NOT_FOUND);
		}

	}
}
