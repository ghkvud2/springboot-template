package com.example.template.domain.user.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.template.global.code.UserErrorCode;
import com.example.template.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class UserApi {

	@GetMapping("/user")
	public ApiResponse<String> findTeamById(String teamId) {
		return ApiResponse.createFail(UserErrorCode.NOT_FOUND);
	}
}
