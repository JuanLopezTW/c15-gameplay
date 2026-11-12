package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.PosicionJugadorDto;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class StompPosicionNotifierTest {

    @Test
    void publicaEnElTopicDelEquipoDeLaPartida() {
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        PosicionJugadorDto dto = new PosicionJugadorDto("u1", 1, 2, true);

        new StompPosicionNotifier(messaging).notificar("p1", "e1", dto);

        verify(messaging).convertAndSend("/topic/partidas/p1/equipos/e1/posiciones", dto);
        verifyNoMoreInteractions(messaging);
    }
}
