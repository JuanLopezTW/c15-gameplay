package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.AsignarRolesUseCase;
import co.eci.c15.gameplay.application.ConsultarRolesUseCase;
import co.eci.c15.gameplay.domain.RolNoAsignadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/partidas/{matchId}")
public class RolesController {

    public record AsignarRequest(Map<String, List<String>> equipos) {}

    private final AsignarRolesUseCase asignar;
    private final ConsultarRolesUseCase consultar;

    public RolesController(AsignarRolesUseCase asignar, ConsultarRolesUseCase consultar) {
        this.asignar = asignar;
        this.consultar = consultar;
    }

    /** Asigna los roles de todos los equipos (el arranque normal lo hace el evento de inicio de partida). */
    @PostMapping("/roles")
    public ResponseEntity<?> asignar(@PathVariable("matchId") String matchId, @RequestBody AsignarRequest body) {
        try {
            return ResponseEntity.ok(asignar.ejecutar(matchId, body.equipos()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/jugadores/{userId}/rol")
    public ResponseEntity<?> rolDe(@PathVariable("matchId") String matchId, @PathVariable("userId") String userId) {
        try {
            return ResponseEntity.ok(consultar.rolDe(matchId, userId));
        } catch (RolNoAsignadoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/equipos/{equipoId}/roles")
    public ResponseEntity<?> rolesDelEquipo(@PathVariable("matchId") String matchId, @PathVariable("equipoId") String equipoId) {
        try {
            return ResponseEntity.ok(consultar.rolesDelEquipo(matchId, equipoId));
        } catch (RolNoAsignadoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
