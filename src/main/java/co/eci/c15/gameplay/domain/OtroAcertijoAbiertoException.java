package co.eci.c15.gameplay.domain;

public class OtroAcertijoAbiertoException extends RuntimeException {
    public OtroAcertijoAbiertoException(String abierto) {
        super("Ya tienes abierto el acertijo '" + abierto + "'; ciérralo antes de abrir otro");
    }
}
