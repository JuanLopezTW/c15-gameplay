package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.MatchTimer;

import java.util.List;
import java.util.Optional;

/** Cronometros de las partidas en curso: basta guardar cuando empezo y cuanto dura. */
public interface CronometrosPartida {

    /** @return false si la partida ya tenia cronometro (gana el primero) */
    boolean crear(MatchTimer cronometro);

    Optional<MatchTimer> find(String matchId);

    /** @return true solo para quien lo quita: el tiempo agotado o la parada se procesan una vez */
    boolean quitar(String matchId);

    /** Partidas con cronometro corriendo. */
    List<String> activos();
}
