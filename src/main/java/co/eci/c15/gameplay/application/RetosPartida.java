package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.ChallengeNotActiveException;
import co.eci.c15.gameplay.domain.ChallengeResult;
import co.eci.c15.gameplay.domain.NotInChallengeException;
import co.eci.c15.gameplay.domain.Rol;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * El reto Heroe vs Verdugo de cada partida. Los cambios de estado son atomicos y pasan una sola vez
 * (PROGRAMADO -> EN_CURSO -> TERMINADO, o PROGRAMADO -> CANCELADO), aunque los intenten varias instancias.
 * Despues de terminar no cuenta ningun click mas: el resultado es exacto.
 */
public interface RetosPartida {

    enum Estado { PROGRAMADO, EN_CURSO, TERMINADO, CANCELADO }

    record Plan(String matchId, List<String> equipos, Instant inicio, Estado estado) {}

    record EnCurso(String matchId, Instant fin, Map<String, Long> puntajes) {}

    /** @return false si la partida ya tenia reto */
    boolean programar(String matchId, List<String> equipos, Instant inicio);

    Optional<Plan> plan(String matchId);

    /** PROGRAMADO -> EN_CURSO; false si no estaba programado (ya empezo, termino o se cancelo). */
    boolean iniciar(String matchId, Map<String, Map<String, Rol>> rolesPorEquipo, Instant fin);

    /**
     * @throws ChallengeNotActiveException si no hay reto en curso o ya paso su hora de fin
     * @throws NotInChallengeException si el jugador no esta en el reto
     */
    void click(String matchId, String userId, Instant ahora);

    Optional<EnCurso> enCurso(String matchId);

    /** EN_CURSO -> TERMINADO con los puntajes finales; vacio si no estaba en curso. */
    Optional<ChallengeResult> terminar(String matchId);

    /** PROGRAMADO -> CANCELADO (la partida termino antes de que empezara). */
    void cancelar(String matchId);

    /** Partidas con el reto programado o en curso. */
    List<String> activos();
}
