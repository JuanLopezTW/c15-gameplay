package co.eci.c15.gameplay.domain;

import java.util.List;

public interface ProgresoRepository {

    /** Devuelve el progreso del equipo, creandolo (con ese total) la primera vez. */
    ProgresoEquipo obtener(String matchId, String equipoId, int total);

    void guardar(ProgresoEquipo progreso);

    /** Progreso de todos los equipos de la partida, ordenados por equipo. */
    List<ProgresoEquipo> findByMatchId(String matchId);
}
