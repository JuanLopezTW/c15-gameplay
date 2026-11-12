package co.eci.c15.gameplay.domain;

public class SolucionNoDisponibleException extends RuntimeException {
    public SolucionNoDisponibleException(String componenteMapaId) {
        super("El acertijo " + componenteMapaId + " no tiene una solucion configurada");
    }
}
