package com.tournament.tournament_manager.infrastructure.ai;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import com.tournament.tournament_manager.domain.port.out.match.GenerateCommentaryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter OpenAI implémentant la génération de commentaire via GPT.
 * Utilise le modèle {@code gpt-4o-mini} pour minimiser les coûts.
 */
@Component
public class OpenAiCommentaryAdapter implements GenerateCommentaryPort {

    private final OpenAiService openAiService;

    public OpenAiCommentaryAdapter(@Value("${openai.api.key}") String apiKey) {
        this.openAiService = new OpenAiService(apiKey);
    }

    /**
     * Génère un commentaire narratif à partir d'un prompt via GPT-4o-mini.
     *
     * @param prompt le prompt décrivant le match
     * @return le commentaire généré
     */
    @Override
    public String generateCommentary(String prompt) {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4o-mini")
                .messages(List.of(new ChatMessage("user", prompt)))
                .maxTokens(200)
                .temperature(0.7)
                .build();

        return openAiService.createChatCompletion(request)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();
    }
}