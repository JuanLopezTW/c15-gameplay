package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.HeroVillainChallengeService;
import co.eci.c15.gameplay.domain.ChallengeNotActiveException;
import co.eci.c15.gameplay.domain.NotInChallengeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

/** Hero vs Executioner challenge (HU-44.5). Live scores go to /topic/partidas/{matchId}/reto. */
@RestController
@RequestMapping("/api/partidas/{matchId}/reto")
public class ChallengeController {

    private final HeroVillainChallengeService challenges;

    public ChallengeController(HeroVillainChallengeService challenges) {
        this.challenges = challenges;
    }

    @PostMapping("/clicks")
    public ResponseEntity<?> click(@PathVariable("matchId") String matchId, Principal yo) {
        try {
            challenges.click(matchId, yo.getName());
            return ResponseEntity.noContent().build();
        } catch (ChallengeNotActiveException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (NotInChallengeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> state(@PathVariable("matchId") String matchId) {
        return challenges.find(matchId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No hay un reto en curso en la partida " + matchId)));
    }
}
