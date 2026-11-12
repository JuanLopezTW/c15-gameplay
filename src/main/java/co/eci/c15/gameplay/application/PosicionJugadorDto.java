package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.Posicion;

import java.util.List;

/**
 * Posicion de un jugador; conectado=false avisa que el jugador salio del mapa. {@code ruta} son
 * las celdas recorridas en el ultimo movimiento (vacia si no viene de un movimiento), para que
 * el front de los compañeros lo anime paso a paso en vez de saltar al destino.
 */
public record PosicionJugadorDto(String userId, int x, int y, boolean conectado, List<Posicion> ruta) {

    public PosicionJugadorDto {
        ruta = ruta == null ? List.of() : List.copyOf(ruta);
    }

    public PosicionJugadorDto(String userId, int x, int y, boolean conectado) {
        this(userId, x, y, conectado, List.of());
    }

    public static PosicionJugadorDto conectado(String userId, Posicion p) {
        return new PosicionJugadorDto(userId, p.x(), p.y(), true);
    }

    /** Posicion tras un movimiento, con la ruta recorrida. */
    public static PosicionJugadorDto movimiento(String userId, Posicion p, List<Posicion> ruta) {
        return new PosicionJugadorDto(userId, p.x(), p.y(), true, ruta);
    }

    public static PosicionJugadorDto desconectado(JugadorEnMapa jugador) {
        Posicion p = jugador.getPosicion();
        return new PosicionJugadorDto(jugador.getUserId(), p.x(), p.y(), false);
    }
}
