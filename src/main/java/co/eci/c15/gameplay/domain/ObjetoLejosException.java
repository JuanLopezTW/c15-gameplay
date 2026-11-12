package co.eci.c15.gameplay.domain;

public class ObjetoLejosException extends RuntimeException {
    public ObjetoLejosException(String componenteMapaId) {
        super("Estas muy lejos de '" + componenteMapaId + "': acercate para recolectarlo");
    }
}
