package co.eci.c15.gameplay.domain.events;

/**
 * Un equipo resolvio un acertijo de su mapa. {@code userId} es quien dio la solucion correcta.
 */
public record AcertijoResueltoEvent(String matchId, String equipoId, String componenteMapaId, String userId) {
}
