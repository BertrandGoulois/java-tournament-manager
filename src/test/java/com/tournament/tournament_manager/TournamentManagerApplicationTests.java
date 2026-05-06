package com.tournament.tournament_manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
		"spring.kafka.bootstrap-servers=localhost:9092",
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
class TournamentManagerApplicationTests {

	@Test
	void contextLoads() {
	}

}
