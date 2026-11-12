package co.eci.c15.gameplay.domain;

import java.util.Optional;

/** Catálogo de acertijos (contenido), uno por componente del mapa. */
public interface AcertijoRepository {
    Optional<Acertijo> findByComponenteMapaId(String componenteMapaId);
    Acertijo save(Acertijo acertijo);
}
