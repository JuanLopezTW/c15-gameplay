package co.eci.c15.gameplay.domain;

public class AcertijoLejosException extends RuntimeException {
    public AcertijoLejosException(String componenteMapaId) {
        super("Debes estar junto al acertijo '" + componenteMapaId + "' para abrirlo");
    }
}
