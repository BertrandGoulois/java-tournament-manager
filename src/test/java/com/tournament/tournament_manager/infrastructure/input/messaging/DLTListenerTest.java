package com.tournament.tournament_manager.infrastructure.input.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;

@ExtendWith(MockitoExtension.class)
class DLTListenerTest {

    @InjectMocks
    private DLTListener dltListener;

    @Test
    void onDeadLetter_shouldLogMessage_whenEventReceived() {
        MatchFinishedEvent event = new MatchFinishedEvent(42L, 0, 0);
        dltListener.onDeadLetter(event);
    }
}
