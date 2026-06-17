package com.tournament.tournament_manager.infrastructure.ai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration du client OpenAI.
 *
 * <p>Externalisé dans une classe de configuration plutôt que construit
 * directement dans l'adapter, afin de permettre l'injection d'un mock
 * en test (notamment pour valider le comportement du circuit breaker).
 */
@Configuration
public class OpenAiConfig {

    @Bean
    public OpenAIClient openAiClient(@Value("${openai.api.key}") String apiKey) {
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}