package co.eci.c15.gameplay.domain;

public class ComponenteSinAcertijoException extends RuntimeException {
    public ComponenteSinAcertijoException(String componenteMapaId) {
        super("El componente '" + componenteMapaId + "' no tiene un acertijo asociado");
    }
}
