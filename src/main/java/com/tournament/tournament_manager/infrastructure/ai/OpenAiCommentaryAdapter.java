package com.tournament.tournament_manager.infrastructure.ai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.tournament.tournament_manager.domain.port.out.match.GenerateCommentaryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Adapter OpenAI implémentant la génération de commentaire via GPT.
 * Utilise le modèle {@code gpt-4o-mini} pour minimiser les coûts.
 */
@Component
public class OpenAiCommentaryAdapter implements GenerateCommentaryPort {

    private final OpenAIClient client;

    public OpenAiCommentaryAdapter(@Value("${openai.api.key}") String apiKey) {
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * Génère un commentaire narratif à partir d'un prompt via GPT-4o-mini.
     *
     * @param prompt le prompt décrivant le match
     * @return le commentaire généré
     */
    @Override
    public String generateCommentary(String prompt) {
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
}