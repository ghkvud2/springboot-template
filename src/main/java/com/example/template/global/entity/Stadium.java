package com.example.template.global.entity;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Stadium {

	private String stadiumId;
	private String stadiumName;
	private String hometeamId;
	private Integer seatCount;
	private String address;
	private String ddd;
	private String tel;
	private List<Schedule> schedules;
}
