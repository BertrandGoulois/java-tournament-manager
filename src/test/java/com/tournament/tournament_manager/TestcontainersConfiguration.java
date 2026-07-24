package com.tournament.tournament_manager;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:16"));
	}

	/*
	 * RedisConfig (rate limiting Bucket4j) lit spring.data.redis.host/port
	 * directement via @Value, sans passer par l'auto-configuration standard
	 * de Spring Boot. @ServiceConnection seul ne suffit donc pas ici (il ne
	 * branche que les beans Redis auto-configures). On force explicitement
	 * les proprietes via DynamicPropertyRegistrar, une fois le container
	 * demarre, pour que RedisConfig pointe vers le bon host/port dynamique
	 * plutot que localhost:6379 (absent sur les runners CI).
	 */
	@Bean
	GenericContainer<?> redisContainer() {
		GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
				.withExposedPorts(6379);
		container.start();
		return container;
	}

	@Bean
	DynamicPropertyRegistrar redisProperties(GenericContainer<?> redisContainer) {
		return registry -> {
			registry.add("spring.data.redis.host", redisContainer::getHost);
			registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
		};
	}
}
