package com.tournament.tournament_manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TournamentManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TournamentManagerApplication.class, args);
	}

}
