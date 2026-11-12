package co.eci.c15.gameplay.infrastructure.web;

import co.eci.c15.gameplay.application.MapaDto;
import co.eci.c15.gameplay.application.ObtenerMapaUseCase;
import co.eci.c15.gameplay.domain.MapaNoGeneradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/partidas/{matchId}/mapa")
public class PartidaMapaController {

    private final ObtenerMapaUseCase obtenerMapa;

    public PartidaMapaController(ObtenerMapaUseCase obtenerMapa) {
        this.obtenerMapa = obtenerMapa;
    }

    @GetMapping
    public ResponseEntity<?> obtener(@PathVariable("matchId") String matchId) {
        try {
            MapaDto dto = obtenerMapa.ejecutar(matchId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (MapaNoGeneradoException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
