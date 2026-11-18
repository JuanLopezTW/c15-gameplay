package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.AbrirAcertijoUseCase;
import co.eci.c15.gameplay.domain.AcertijoBloqueadoException;
import co.eci.c15.gameplay.domain.AcertijoLejosException;
import co.eci.c15.gameplay.domain.ComponenteSinAcertijoException;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import co.eci.c15.gameplay.domain.OtroAcertijoAbiertoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/partidas/{matchId}/acertijos/{componenteMapaId}")
public class AcertijoController {

    private static final String ERROR = "error";

    private final AbrirAcertijoUseCase useCase;

    public AcertijoController(AbrirAcertijoUseCase useCase) {
        this.useCase = useCase;
    }

    /** Abre el acertijo (Enter junto al punto brillante) y lo bloquea para el resto del equipo. */
    @PostMapping("/bloqueo")
    public ResponseEntity<Object> abrir(@PathVariable("matchId") String matchId,
                                   @PathVariable("componenteMapaId") String componenteMapaId, Principal yo) {
        try {
            return ResponseEntity.ok(useCase.ejecutar(matchId, componenteMapaId, yo.getName()));
        } catch (JugadorNoRegistradoException | ComponenteSinAcertijoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(ERROR, e.getMessage()));
        } catch (AcertijoBloqueadoException | OtroAcertijoAbiertoException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(ERROR, e.getMessage()));
        } catch (AcertijoLejosException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR, e.getMessage()));
        }
    }

    /** Cierra el acertijo y libera el bloqueo. */
    /**
     * Mira el acertijo sin abrirlo ni bloquearlo: el companero ve sus propias pistas mientras el otro
     * lo tiene abierto. No pide estar al lado: solo muestra lo que ya es de su equipo.
     */
    @GetMapping
    public ResponseEntity<Object> ver(@PathVariable("matchId") String matchId,
                                      @PathVariable("componenteMapaId") String componenteMapaId, Principal yo) {
        try {
            return ResponseEntity.ok(useCase.ver(matchId, componenteMapaId, yo.getName()));
        } catch (JugadorNoRegistradoException | ComponenteSinAcertijoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(ERROR, e.getMessage()));
        }
    }

    @DeleteMapping("/bloqueo")
    public ResponseEntity<Object> cerrar(@PathVariable("matchId") String matchId,
                                    @PathVariable("componenteMapaId") String componenteMapaId, Principal yo) {
        try {
            useCase.cerrar(matchId, componenteMapaId, yo.getName());
            return ResponseEntity.noContent().build();
        } catch (JugadorNoRegistradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(ERROR, e.getMessage()));
        }
    }
}
