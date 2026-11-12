package co.eci.c15.gameplay.domain;

public interface InventarioRepository {

    /** Devuelve el inventario del equipo en la partida, creandolo vacio la primera vez. */
    InventarioEquipo obtener(String matchId, String equipoId);

    void guardar(InventarioEquipo inventario);
}
