package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.AccesoEquipoDenegadoException;
import co.eci.c15.gameplay.application.ConsultarPosicionesEquipoUseCase;
import co.eci.c15.gameplay.application.DesconectarJugadorUseCase;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/partidas/{matchId}")
public class PosicionesEquipoController {

    private final ConsultarPosicionesEquipoUseCase consultar;
    private final DesconectarJugadorUseCase desconectar;

    public PosicionesEquipoController(ConsultarPosicionesEquipoUseCase consultar,
                                      DesconectarJugadorUseCase desconectar) {
        this.consultar = consultar;
        this.desconectar = desconectar;
    }

    /** Posiciones de los companeros de equipo; solo las puede ver un miembro del mismo equipo. */
    @GetMapping("/equipos/{equipoId}/posiciones")
    public ResponseEntity<?> posiciones(@PathVariable("matchId") String matchId,
                                        @PathVariable("equipoId") String equipoId, Principal yo) {
        try {
            return ResponseEntity.ok(consultar.ejecutar(matchId, equipoId, yo.getName()));
        } catch (JugadorNoRegistradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (AccesoEquipoDenegadoException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    /** Desconexion explicita (salir de la partida). */
    @DeleteMapping("/jugadores/{userId}")
    public ResponseEntity<Void> desconectar(@PathVariable("matchId") String matchId,
                                            @PathVariable("userId") String userId, Principal yo) {
        if (!userId.equals(yo.getName())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        desconectar.ejecutar(matchId, userId);
        return ResponseEntity.noContent().build();
    }
}
