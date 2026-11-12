package co.eci.c15.gameplay.domain;

public class AcertijoYaResueltoException extends RuntimeException {
    public AcertijoYaResueltoException(String componenteMapaId) {
        super("El acertijo " + componenteMapaId + " ya fue resuelto por el equipo");
    }
}
