package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.gameplay.domain.events.AcertijoResueltoEvent;
import co.eci.c15.gameplay.domain.events.ChallengeFinishedEvent;
import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.gameplay.domain.events.TimeUpEvent;
import co.eci.c15.gameplay.application.MatchResultService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

class MatchResultListenerTest {

    private final MatchResultService service = mock(MatchResultService.class);
    private final MatchResultListener listener = new MatchResultListener(service);

    @Test
    void forwardsTheMatchEvents() {
        Map<String, List<String>> equipos = Map.of("A", List.of("ana"), "B", List.of("caro"));
        listener.onPartidaIniciada(new PartidaIniciadaEvent("m1", equipos));
        verify(service).matchStarted("m1", equipos);

        ChallengeFinishedEvent challenge = new ChallengeFinishedEvent("m1", "A", Map.of("A", 1L, "B", 0L));
        listener.onChallengeFinished(challenge);
        verify(service).challengeFinished(challenge);

        AcertijoResueltoEvent solved = new AcertijoResueltoEvent("m1", "A", "acertijo-1", "ana");
        listener.onAcertijoResuelto(solved);
        verify(service).puzzleSolved(solved);

        listener.onTimeUp(new TimeUpEvent("m1", Instant.now()));
        verify(service).timeUp("m1");
    }
}
