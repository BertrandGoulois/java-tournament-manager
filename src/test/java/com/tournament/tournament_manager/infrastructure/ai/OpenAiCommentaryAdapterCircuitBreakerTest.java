package com.tournament.tournament_manager.infrastructure.ai;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.tournament.tournament_manager.TestcontainersConfiguration;
import com.tournament.tournament_manager.exception.OpenAiUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Vérifie le comportement du circuit breaker Resilience4j protégeant l'appel OpenAI.
 *
 * <p>Les seuils sont surchargés via {@code @TestPropertySource} pour accélérer
 * les tests (fenêtre glissante réduite, délai d'ouverture court).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "resilience4j.circuitbreaker.instances.openai.sliding-window-size=4",
        "resilience4j.circuitbreaker.instances.openai.minimum-number-of-calls=4",
        "resilience4j.circuitbreaker.instances.openai.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.openai.wait-duration-in-open-state=1s",
        "resilience4j.circuitbreaker.instances.openai.permitted-number-of-calls-in-half-open-state=2",
        "resilience4j.circuitbreaker.instances.openai.automatic-transition-from-open-to-half-open-enabled=true"
})
class OpenAiCommentaryAdapterCircuitBreakerTest {

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    private OpenAIClient openAIClient;

    @Autowired
    private OpenAiCommentaryAdapter adapter;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("openai").reset();
    }

    @Test
    void circuitBreaker_shouldOpen_afterRepeatedFailures() {
        when(openAIClient.chat().completions().create(any(ChatCompletionCreateParams.class)))
                .thenThrow(new RuntimeException("OpenAI API down"));

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("openai");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        for (int i = 0; i < 4; i++) {
            int idx = i;
            assertThrows(OpenAiUnavailableException.class,
                    () -> adapter.generateCommentary("prompt " + idx));
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void circuitBreaker_shouldShortCircuit_whenOpen_withoutCallingClient() {
        when(openAIClient.chat().completions().create(any(ChatCompletionCreateParams.class)))
                .thenThrow(new RuntimeException("OpenAI API down"));

        for (int i = 0; i < 4; i++) {
            int idx = i;
            assertThrows(OpenAiUnavailableException.class,
                    () -> adapter.generateCommentary("prompt " + idx));
        }

        clearInvocations(openAIClient);

        assertThrows(OpenAiUnavailableException.class,
                () -> adapter.generateCommentary("should be short-circuited"));

        verify(openAIClient, never()).chat();
    }

    @Test
    void circuitBreaker_shouldTransitionToHalfOpen_afterWaitDuration() throws InterruptedException {
        when(openAIClient.chat().completions().create(any(ChatCompletionCreateParams.class)))
                .thenThrow(new RuntimeException("OpenAI API down"));

        for (int i = 0; i < 4; i++) {
            int idx = i;
            assertThrows(OpenAiUnavailableException.class,
                    () -> adapter.generateCommentary("prompt " + idx));
        }

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("openai");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        Thread.sleep(1100); // attendre le wait-duration-in-open-state (1s)

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
    }
}