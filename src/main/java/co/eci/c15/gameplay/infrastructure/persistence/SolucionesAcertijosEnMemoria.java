package co.eci.c15.gameplay.infrastructure.persistence;

import co.eci.c15.gameplay.domain.SolucionAcertijoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Soluciones de los acertijos de ejemplo (acertijo-1..3 del mapa), que acompanan a los
 * enunciados de {@link AcertijosPlaceholderLoader}. Reemplazar junto con el contenido real.
 */
@Repository
public class SolucionesAcertijosEnMemoria implements SolucionAcertijoRepository {

    private static final Map<String, List<String>> SOLUCIONES = Map.of(
            "acertijo-1", List.of("16"),
            "acertijo-2", List.of("rojo", "naranja", "amarillo", "verde", "azul", "añil", "violeta"),
            "acertijo-3", List.of("escape"));

    @Override
    public Optional<List<String>> findByComponenteMapaId(String componenteMapaId) {
        return Optional.ofNullable(SOLUCIONES.get(componenteMapaId));
    }
}
