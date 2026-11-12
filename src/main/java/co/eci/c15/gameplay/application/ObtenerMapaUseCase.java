package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.GeneradorMapa;
import co.eci.c15.gameplay.domain.MapaIsometrico;
import co.eci.c15.gameplay.domain.MapaRepository;
import org.springframework.stereotype.Service;

/**
 * Devuelve el mapa de la partida, generandolo la primera vez. Es idempotente y seguro
 * ante llamadas concurrentes: todos reciben el mismo mapa.
 */
@Service
public class ObtenerMapaUseCase {

    private final MapaRepository mapas;
    private final GeneradorMapa generador;

    public ObtenerMapaUseCase(MapaRepository mapas, GeneradorMapa generador) {
        this.mapas = mapas;
        this.generador = generador;
    }

    public MapaDto ejecutar(String matchId) {
        return MapaDto.from(obtener(matchId));
    }

    public MapaIsometrico obtener(String matchId) {
        if (matchId == null || matchId.isBlank()) throw new IllegalArgumentException("El id de la partida no puede estar vacio");
        return mapas.findByMatchId(matchId)
                .orElseGet(() -> mapas.saveIfAbsent(generador.generar(matchId)));
    }
}
