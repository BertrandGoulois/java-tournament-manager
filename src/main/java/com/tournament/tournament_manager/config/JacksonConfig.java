package com.tournament.tournament_manager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Jackson.
 *
 * <p>Expose un bean {@link ObjectMapper} partagé entre tous les composants
 * qui en ont besoin (handlers JSON-RPC, etc.), évitant la création de
 * multiples instances coûteuses via {@code new ObjectMapper()}.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}