package co.eci.c15.gameplay.domain;

import java.util.List;
import java.util.Optional;

/**
 * Que acertijo hay en cada punto del mapa de una partida. La eleccion es determinista por matchId:
 * los dos equipos y todas las replicas ven los mismos acertijos sin guardar nada.
 */
public interface CatalogoAcertijos {

    Optional<DefinicionAcertijo> de(String matchId, String componenteMapaId);

    /** Objetos que el mapa de la partida debe tener para sus acertijos (ej. la lampara UV). */
    List<String> objetosDe(String matchId);
}
