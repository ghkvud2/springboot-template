package com.example.template.domain.team.api;

import org.hibernate.validator.constraints.Length;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.template.domain.team.exception.TeamException;
import com.example.template.domain.team.mapper.TeamMapper;
import com.example.template.global.entity.Team;
import com.example.template.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@Validated
@RequiredArgsConstructor
@RestController
public class TeamApi {

	private final TeamMapper teamMapper;

	@GetMapping("/team")
	public ApiResponse<Team> findTeamById(@Length(min = 3, message = "팀ID는 최소 세 글자입니다.") String teamId) {

		Team team = teamMapper.findTeamById(teamId).orElseThrow(TeamException.TeamNotFoundException::new);
		return ApiResponse.createSuccess(team);
	}
}
