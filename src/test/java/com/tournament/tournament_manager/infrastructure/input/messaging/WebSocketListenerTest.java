package com.tournament.tournament_manager.infrastructure.input.messaging;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketListener webSocketListener;

    @Test
    void onMatchFinished_shouldBroadcastEvent() {
        MatchFinishedEvent event = new MatchFinishedEvent(1L, 0, 0);

        webSocketListener.onMatchFinished(event);

        verify(messagingTemplate, times(1)).convertAndSend("/topic/matches", event);
    }
}