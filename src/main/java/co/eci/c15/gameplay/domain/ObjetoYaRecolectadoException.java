package co.eci.c15.gameplay.domain;

public class ObjetoYaRecolectadoException extends RuntimeException {
    public ObjetoYaRecolectadoException(String componenteMapaId) {
        super("Tu equipo ya recolecto '" + componenteMapaId + "'");
    }
}
