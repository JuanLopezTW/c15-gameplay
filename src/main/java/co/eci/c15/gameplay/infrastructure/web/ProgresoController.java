package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.ProgresoPartidaDto;
import co.eci.c15.gameplay.application.ProgresoPartidaUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/partidas/{matchId}/progreso")
public class ProgresoController {

    private final ProgresoPartidaUseCase useCase;

    public ProgresoController(ProgresoPartidaUseCase useCase) {
        this.useCase = useCase;
    }

    /** Progreso actual de todos los equipos (para pintar el marcador al entrar o recargar). */
    @GetMapping
    public ResponseEntity<?> consultar(@PathVariable("matchId") String matchId) {
        ProgresoPartidaDto progreso = useCase.consultar(matchId);
        if (progreso.equipos().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "La partida " + matchId + " no tiene progreso registrado"));
        }
        return ResponseEntity.ok(progreso);
    }
}
