package com.tournament.tournament_manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Configuration Jackson.
 *
 * <p>Expose un bean {@link ObjectMapper} (Jackson 3) partagé entre tous les composants
 * qui en ont besoin (handlers JSON-RPC, etc.), évitant la création de
 * multiples instances coûteuses.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder().build();
    }
}
