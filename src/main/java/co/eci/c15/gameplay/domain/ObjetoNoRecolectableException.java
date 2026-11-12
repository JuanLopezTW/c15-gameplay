package co.eci.c15.gameplay.domain;

public class ObjetoNoRecolectableException extends RuntimeException {
    public ObjetoNoRecolectableException(String componenteMapaId) {
        super("'" + componenteMapaId + "' no es un objeto que se pueda recolectar");
    }
}
