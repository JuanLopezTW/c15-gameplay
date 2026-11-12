package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.gameplay.application.MatchTimerService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

class MatchTimerListenerTest {

    @Test
    void startsTheTimerWithTheDurationChosenByTheHost() {
        MatchTimerService timers = mock(MatchTimerService.class);
        MatchTimerListener listener = new MatchTimerListener(timers);

        listener.onPartidaIniciada(new PartidaIniciadaEvent("match-1", Map.of("e1", List.of("ana", "beto")),
                Duration.ofMinutes(15)));

        verify(timers).start("match-1", Duration.ofMinutes(15));
    }
}
