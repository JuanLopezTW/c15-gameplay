package co.eci.c15.gameplay.domain;

import java.util.List;
import java.util.Optional;

/** Solucion de cada acertijo del catalogo, como lista de pasos en orden. */
public interface SolucionAcertijoRepository {
    Optional<List<String>> findByComponenteMapaId(String componenteMapaId);
}
