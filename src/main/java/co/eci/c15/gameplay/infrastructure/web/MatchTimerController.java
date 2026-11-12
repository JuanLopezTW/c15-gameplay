package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.MatchTimerService;
import co.eci.c15.gameplay.application.TimerState;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/partidas")
public class MatchTimerController {

    private final MatchTimerService timers;

    public MatchTimerController(MatchTimerService timers) {
        this.timers = timers;
    }

    @GetMapping("/{matchId}/cronometro")
    public ResponseEntity<TimerState> timer(@PathVariable String matchId) {
        return ResponseEntity.of(timers.find(matchId));
    }
}
