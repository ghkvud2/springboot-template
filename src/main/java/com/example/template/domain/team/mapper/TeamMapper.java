package com.example.template.domain.team.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;

import com.example.template.global.entity.Team;

@Mapper
public interface TeamMapper {
	
	Optional<Team> findTeamById(String teamId);
}