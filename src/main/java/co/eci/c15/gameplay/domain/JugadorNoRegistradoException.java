package co.eci.c15.gameplay.domain;

public class JugadorNoRegistradoException extends RuntimeException {
    public JugadorNoRegistradoException(String matchId, String userId) {
        super("El jugador " + userId + " no esta en el mapa de la partida " + matchId);
    }
}
