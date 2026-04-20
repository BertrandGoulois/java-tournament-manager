package com.tournament.tournament_manager;

import org.springframework.boot.SpringApplication;

public class TestTournamentManagerApplication {

	public static void main(String[] args) {
		SpringApplication.from(TournamentManagerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
