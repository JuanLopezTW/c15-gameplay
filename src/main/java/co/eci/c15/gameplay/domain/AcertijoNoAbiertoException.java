package co.eci.c15.gameplay.domain;

public class AcertijoNoAbiertoException extends RuntimeException {
    public AcertijoNoAbiertoException(String componenteMapaId, String userId) {
        super("El jugador " + userId + " no tiene abierto el acertijo " + componenteMapaId);
    }
}
