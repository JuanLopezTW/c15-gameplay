package co.eci.c15.gameplay.application;

/**
 * La conexion mas reciente de cada jugador en el mapa de una partida. Sirve para la gracia al desconectarse:
 * si el jugador recarga la pagina o se le cae la red y vuelve (abre otra conexion), la vieja ya no es la ultima y
 * no lo saca de la partida.
 */
public interface SesionesEnMapa {

    /** El broker avisa (y repite cada 30 s) que esta conexion del jugador esta abierta en el mapa. */
    void abierta(String matchId, String userId, String sessionId);

    /** Si la conexion que se cerro sigue siendo la ultima del jugador (no volvio con otra). */
    boolean esLaUltima(String matchId, String userId, String sessionId);
}
