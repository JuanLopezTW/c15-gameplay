package co.eci.c15.gameplay.domain;

public class NotInChallengeException extends RuntimeException {
    public NotInChallengeException(String matchId, String userId) {
        super("El jugador " + userId + " no participa en el reto de la partida " + matchId);
    }
}
