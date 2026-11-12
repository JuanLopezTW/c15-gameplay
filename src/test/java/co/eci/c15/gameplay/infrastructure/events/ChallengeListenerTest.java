package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.gameplay.domain.events.TimeUpEvent;
import co.eci.c15.gameplay.application.HeroVillainChallengeService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;

class ChallengeListenerTest {

    private final HeroVillainChallengeService service = mock(HeroVillainChallengeService.class);
    private final ChallengeListener listener = new ChallengeListener(service);

    @Test
    void schedulesTheChallengeWhenTheMatchBegins() {
        listener.onPartidaIniciada(new PartidaIniciadaEvent("m1", Map.of("A", List.of("ana"), "B", List.of("caro")),
                Duration.ofMinutes(10)));
        verify(service).schedule("m1", Set.of("A", "B"), Duration.ofMinutes(10));
    }

    @Test
    void closesTheChallengeWhenTimeIsUp() {
        listener.onTimeUp(new TimeUpEvent("m1", Instant.now()));
        verify(service).matchOver("m1");
    }
}
