package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.HeroVillainChallengeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Solo para desarrollo: dispara ya el reto de una partida iniciada, sin esperar el momento
 * aleatorio (sirve para probar el front del reto).
 */
@RestController
@RequestMapping("/dev/partidas")
public class DevChallengeController {

    private final HeroVillainChallengeService challenges;

    public DevChallengeController(HeroVillainChallengeService challenges) {
        this.challenges = challenges;
    }

    @PostMapping("/{matchId}/reto/inicio")
    public ResponseEntity<?> start(@PathVariable("matchId") String matchId) {
        if (challenges.start(matchId)) return ResponseEntity.ok(challenges.find(matchId).orElseThrow());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "La partida " + matchId + " no existe o ya tuvo su reto"));
    }
}
