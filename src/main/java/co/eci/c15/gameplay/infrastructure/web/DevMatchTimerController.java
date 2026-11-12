package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.MatchTimerService;
import co.eci.c15.gameplay.application.TimerState;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Solo para desarrollo: arranca un cronometro sin armar una sala. En el juego real el
 * cronometro lo arranca MatchTimerListener con PartidaIniciadaEvent (HU-64.4).
 */
@RestController
@RequestMapping("/dev/partidas")
public class DevMatchTimerController {

    private final MatchTimerService timers;

    public DevMatchTimerController(MatchTimerService timers) {
        this.timers = timers;
    }

    @PostMapping("/{matchId}/inicio")
    public TimerState start(@PathVariable String matchId,
                            @RequestParam(name = "segundos", defaultValue = "60") long seconds) {
        timers.start(matchId, Duration.ofSeconds(seconds));
        return timers.find(matchId).orElse(new TimerState(matchId, 0, true));
    }
}