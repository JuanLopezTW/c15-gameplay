package co.eci.c15.gameplay.domain;

public class AcertijoBloqueadoException extends RuntimeException {
    public AcertijoBloqueadoException(String componenteMapaId) {
        super("El acertijo '" + componenteMapaId + "' está siendo resuelto por otro jugador de tu equipo");
    }
}
