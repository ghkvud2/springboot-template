package com.example.template.global.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Schedule {

	private String stadiumId;
	private String scheDate;
	private String gubun;
	private String hometeamId;
	private String awayteamId;
	private Integer homeScore;
	private Integer awayScore;
}