package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.AccesoEquipoDenegadoException;
import co.eci.c15.gameplay.application.ConsultarInventarioUseCase;
import co.eci.c15.gameplay.application.RecolectarObjetoUseCase;
import co.eci.c15.gameplay.domain.JugadorNoRegistradoException;
import co.eci.c15.gameplay.domain.ObjetoLejosException;
import co.eci.c15.gameplay.domain.ObjetoNoRecolectableException;
import co.eci.c15.gameplay.domain.ObjetoYaRecolectadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/partidas/{matchId}")
public class InventarioController {

    private final RecolectarObjetoUseCase recolectar;
    private final ConsultarInventarioUseCase consultar;

    public InventarioController(RecolectarObjetoUseCase recolectar, ConsultarInventarioUseCase consultar) {
        this.recolectar = recolectar;
        this.consultar = consultar;
    }

    /** Recolecta un objeto del mapa y lo agrega al inventario del equipo completo. */
    @PostMapping("/objetos/{componenteMapaId}/recoleccion")
    public ResponseEntity<Object> recolectar(@PathVariable("matchId") String matchId,
                                        @PathVariable("componenteMapaId") String componenteMapaId, Principal yo) {
        try {
            return ResponseEntity.ok(recolectar.ejecutar(matchId, yo.getName(), componenteMapaId));
        } catch (JugadorNoRegistradoException | ObjetoNoRecolectableException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (ObjetoLejosException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (ObjetoYaRecolectadoException e) {
            return error(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    /** Inventario actual del equipo (para pintarlo al entrar o recargar). */
    @GetMapping("/equipos/{equipoId}/inventario")
    public ResponseEntity<Object> inventario(@PathVariable("matchId") String matchId,
                                        @PathVariable("equipoId") String equipoId, Principal yo) {
        try {
            return ResponseEntity.ok(consultar.ejecutar(matchId, equipoId, yo.getName()));
        } catch (JugadorNoRegistradoException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (AccesoEquipoDenegadoException e) {
            return error(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    private static ResponseEntity<Object> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("error", mensaje));
    }
}
