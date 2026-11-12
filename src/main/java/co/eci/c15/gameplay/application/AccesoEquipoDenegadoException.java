package co.eci.c15.gameplay.application;

public class AccesoEquipoDenegadoException extends RuntimeException {
    public AccesoEquipoDenegadoException(String userId, String equipoId) {
        super("El jugador " + userId + " no pertenece al equipo " + equipoId);
    }
}
