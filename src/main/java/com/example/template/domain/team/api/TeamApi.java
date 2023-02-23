package com.example.template.domain.team.api;

import org.hibernate.validator.constraints.Length;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.template.domain.team.exception.TeamException;
import com.example.template.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@Validated
@RequiredArgsConstructor
@RestController
public class TeamApi {

	@GetMapping("/team")
	public ApiResponse<String> findTeamById(@Length(min = 3, message = "팀ID는 최소 세 글자입니다.") String teamId) {

		if (teamId == null) {
			throw new TeamException.TeamNotFoundException();
		}

		return ApiResponse.createSuccess();
	}
}
