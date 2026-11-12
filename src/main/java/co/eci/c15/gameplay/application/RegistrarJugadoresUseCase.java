package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.JugadorEnMapaRepository;
import co.eci.c15.gameplay.domain.MapaIsometrico;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** Coloca a todos los jugadores de la partida en el punto de aparicion del mapa. */
@Service
public class RegistrarJugadoresUseCase {

    private final JugadorEnMapaRepository jugadores;
    private final ObtenerMapaUseCase obtenerMapa;

    public RegistrarJugadoresUseCase(JugadorEnMapaRepository jugadores, ObtenerMapaUseCase obtenerMapa) {
        this.jugadores = jugadores;
        this.obtenerMapa = obtenerMapa;
    }

    public void ejecutar(String matchId, Map<String, List<String>> equipos) {
        MapaIsometrico mapa = obtenerMapa.obtener(matchId);
        equipos.forEach((equipoId, miembros) -> miembros.forEach(userId ->
                jugadores.saveIfAbsent(new JugadorEnMapa(matchId, equipoId, userId, mapa.getSpawn()))));
    }
}
