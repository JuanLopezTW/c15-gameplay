package co.eci.c15.gameplay.domain;

public class ElementoNoValidoException extends RuntimeException {
    public ElementoNoValidoException(String componenteMapaId, String elementoId) {
        super("El elemento " + elementoId + " no pertenece al acertijo " + componenteMapaId);
    }
}
