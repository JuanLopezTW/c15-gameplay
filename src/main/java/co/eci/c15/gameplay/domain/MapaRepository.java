package co.eci.c15.gameplay.domain;

import java.util.Optional;

public interface MapaRepository {
    Optional<MapaIsometrico> findByMatchId(String matchId);

    /** Guarda el mapa solo si la partida aun no tiene uno; devuelve el que quedo guardado. */
    MapaIsometrico saveIfAbsent(MapaIsometrico mapa);
}
