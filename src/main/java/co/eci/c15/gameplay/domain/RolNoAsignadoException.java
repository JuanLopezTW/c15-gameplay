package co.eci.c15.gameplay.domain;

public class RolNoAsignadoException extends RuntimeException {
    public RolNoAsignadoException(String matchId, String userId) {
        super("El jugador " + userId + " no tiene rol en la partida " + matchId);
    }
}
