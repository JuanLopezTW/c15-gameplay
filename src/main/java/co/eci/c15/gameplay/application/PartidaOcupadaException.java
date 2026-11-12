package co.eci.c15.gameplay.application;

/** No se pudo tomar el candado del equipo a tiempo (muchos cambios a la vez); el cliente puede reintentar. */
public class PartidaOcupadaException extends RuntimeException {
    public PartidaOcupadaException(String matchId) {
        super("La partida " + matchId + " está ocupada, intenta de nuevo");
    }
}
