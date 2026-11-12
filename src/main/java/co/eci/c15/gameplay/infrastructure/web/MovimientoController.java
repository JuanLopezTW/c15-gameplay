package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.MoverPersonajeUseCase;
import co.eci.c15.gameplay.application.MovimientoDto;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import co.eci.c15.gameplay.domain.MovimientoInvalidoException;
import co.eci.c15.gameplay.domain.Posicion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/partidas/{matchId}/jugadores/{userId}")
public class MovimientoController {

    private static final String ERROR = "error";

    public record MoverRequest(int x, int y) {}

    private final MoverPersonajeUseCase mover;

    public MovimientoController(MoverPersonajeUseCase mover) {
        this.mover = mover;
    }

    @PostMapping("/movimiento")
    public ResponseEntity<Object> mover(@PathVariable("matchId") String matchId,
                                   @PathVariable("userId") String userId,
                                   @RequestBody MoverRequest body, Principal yo) {
        if (!userId.equals(yo.getName())) {
            return ResponseEntity.status(403).body(Map.of(ERROR, "Solo puedes mover a tu propio personaje"));
        }
        try {
            MovimientoDto dto = mover.ejecutar(matchId, userId, new Posicion(body.x(), body.y()));
            return ResponseEntity.ok(dto);
        } catch (JugadorNoRegistradoException e) {
            return ResponseEntity.status(404).body(Map.of(ERROR, e.getMessage()));
        } catch (MovimientoInvalidoException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR, e.getMessage()));
        }
    }
}
