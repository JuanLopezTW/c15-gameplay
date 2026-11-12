package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.MatchResultService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Final result of a match (HU-67.6). It is also pushed to /topic/partidas/{matchId}/resultado. */
@RestController
@RequestMapping("/api/partidas/{matchId}/resultado")
public class MatchResultController {

    private final MatchResultService results;

    public MatchResultController(MatchResultService results) {
        this.results = results;
    }

    @GetMapping
    public ResponseEntity<?> result(@PathVariable("matchId") String matchId) {
        return results.find(matchId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "La partida " + matchId + " no ha terminado")));
    }
}
