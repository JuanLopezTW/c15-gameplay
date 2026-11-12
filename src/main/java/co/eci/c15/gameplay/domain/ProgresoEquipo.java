package co.eci.c15.gameplay.domain;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Acertijos que un equipo lleva resueltos en una partida, sobre el total de su mapa. */
public final class ProgresoEquipo {

    private final String matchId;
    private final String equipoId;
    private final int total;
    private final Set<String> resueltos = new LinkedHashSet<>();

    public ProgresoEquipo(String matchId, String equipoId, int total) {
        if (total < 0) throw new IllegalArgumentException("El total de acertijos no puede ser negativo");
        this.matchId = Objects.requireNonNull(matchId);
        this.equipoId = Objects.requireNonNull(equipoId);
        this.total = total;
    }

    /** Reconstruye el progreso tal como quedo guardado (estado compartido entre instancias). */
    public static ProgresoEquipo restaurar(String matchId, String equipoId, int total, List<String> resueltos) {
        ProgresoEquipo progreso = new ProgresoEquipo(matchId, equipoId, total);
        resueltos.forEach(progreso::registrarResuelto);
        return progreso;
    }

    /** @return true si es un acertijo nuevo para el equipo; repetirlo no cambia nada */
    public synchronized boolean registrarResuelto(String componenteMapaId) {
        return resueltos.add(Objects.requireNonNull(componenteMapaId));
    }

    public synchronized List<String> getResueltos() { return List.copyOf(resueltos); }
    public synchronized int cantidadResueltos() { return resueltos.size(); }
    public synchronized boolean estaCompleto() { return total > 0 && resueltos.size() >= total; }

    public String getMatchId() { return matchId; }
    public String getEquipoId() { return equipoId; }
    public int getTotal() { return total; }
}
