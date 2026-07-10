package com.tournament.tournament_manager.infrastructure.output.client;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.tournament.tournament_manager.domain.port.out.match.GenerateCommentaryPort;
import com.tournament.tournament_manager.exception.domain.OpenAiUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter OpenAI implémentant la génération de commentaire via GPT.
 * Utilise le modèle {@code gpt-4o-mini} pour minimiser les coûts.
 *
 * <p>Protégé par un circuit breaker Resilience4j : après une série d'échecs,
 * les appels suivants sont court-circuités vers {@link #fallbackGenerateCommentary}
 * sans solliciter l'API OpenAI, afin d'éviter de saturer l'application en cas
 * d'indisponibilité prolongée du service externe.
 */
@Slf4j
@Component
public class OpenAiCommentaryAdapter implements GenerateCommentaryPort {

    private final OpenAIClient client;

    public OpenAiCommentaryAdapter(OpenAIClient client) {
        this.client = client;
    }

    /**
     * Génère un commentaire narratif à partir d'un prompt via GPT-4o-mini.
     *
     * @param prompt le prompt décrivant le match
     * @return le commentaire généré
     */
    @Override
    @CircuitBreaker(name = "openai", fallbackMethod = "fallbackGenerateCommentary")
    public String generateCommentary(String prompt) {
        log.debug("Appel OpenAI pour génération de commentaire");
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model("gpt-4o-mini")
                .addUserMessage(prompt)
                .maxCompletionTokens(200)
                .build();

        ChatCompletion completion = client.chat().completions().create(params);

        return completion.choices()
                .get(0)
                .message()
                .content()
                .orElse("");
    }

    /**
     * Fallback invoqué lorsque le circuit breaker est ouvert ou que l'appel échoue.
     * Relance une exception dédiée, capturée par {@code CommentaryListener}
     * comme tout autre échec de génération.
     *
     * @param prompt le prompt qui aurait été envoyé
     * @param t      la cause de l'échec
     */
    private String fallbackGenerateCommentary(String prompt, Throwable t) {
        log.warn("Circuit breaker OpenAI déclenché - commentaire non généré. Cause : {}", t.getMessage());
        throw new OpenAiUnavailableException("Service de commentaire IA indisponible", t);
    }
}