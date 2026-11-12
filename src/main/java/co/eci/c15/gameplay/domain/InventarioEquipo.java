package co.eci.c15.gameplay.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Inventario compartido de un equipo: lo que recolecta cualquiera de sus miembros es de todos.
 * Cada objeto se puede recolectar una sola vez; si dos companeros lo intentan a la vez, solo
 * uno lo consigue. Los metodos son atomicos.
 */
public final class InventarioEquipo {

    private final String matchId;
    private final String equipoId;
    private final Map<String, ObjetoInventario> objetos = new LinkedHashMap<>();

    public InventarioEquipo(String matchId, String equipoId) {
        this.matchId = Objects.requireNonNull(matchId);
        this.equipoId = Objects.requireNonNull(equipoId);
    }

    /** Reconstruye el inventario tal como quedo guardado (estado compartido entre instancias). */
    public static InventarioEquipo restaurar(String matchId, String equipoId, List<ObjetoInventario> objetos) {
        InventarioEquipo inventario = new InventarioEquipo(matchId, equipoId);
        objetos.forEach(inventario::agregar);
        return inventario;
    }

    /** @return true si el objeto es nuevo para el equipo; false si ya lo tenian */
    public synchronized boolean agregar(ObjetoInventario objeto) {
        return objetos.putIfAbsent(objeto.id(), objeto) == null;
    }

    /** Objetos en el orden en que se recolectaron. */
    public synchronized List<ObjetoInventario> getObjetos() { return List.copyOf(objetos.values()); }

    public String getMatchId() { return matchId; }
    public String getEquipoId() { return equipoId; }
}
