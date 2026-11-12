package co.eci.c15.gameplay.domain;

public class ChallengeNotActiveException extends RuntimeException {
    public ChallengeNotActiveException(String matchId) {
        super("No hay un reto Heroe vs Verdugo en curso en la partida " + matchId);
    }
}
