package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.Posicion;

import java.util.List;

/**
 * Un jugador cambio de posicion en el mapa de su partida. {@code ruta} son las celdas que
 * recorrio (termina en {@code posicion}) para que los compañeros puedan animar el movimiento.
 */
public record PosicionActualizadaEvent(String matchId, String equipoId, String userId, Posicion posicion,
                                       List<Posicion> ruta) {
}
