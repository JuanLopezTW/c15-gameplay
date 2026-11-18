package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.CatalogoAcertijos;
import co.eci.c15.gameplay.domain.DefinicionAcertijo;
import co.eci.c15.gameplay.domain.ElementoInteractivo;
import co.eci.c15.gameplay.infrastructure.persistence.InventarioRepositoryEnMemoria;
import co.eci.c15.gameplay.infrastructure.persistence.RolesRepositoryEnMemoria;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Catalogo fijo para las pruebas de los casos de uso: los mismos acertijos en todas las partidas. */
final class CatalogoDePrueba implements CatalogoAcertijos {

    private final Map<String, DefinicionAcertijo> porComponente = new HashMap<>();

    CatalogoDePrueba con(String componenteMapaId, DefinicionAcertijo acertijo) {
        porComponente.put(componenteMapaId, acertijo);
        return this;
    }

    /** Un acertijo de un solo elemento y una sola respuesta. */
    static DefinicionAcertijo simple(String id, String enunciado, String elementoId, String tipoElemento,
                                     List<String> solucion) {
        return new DefinicionAcertijo(id, "pregunta", enunciado,
                List.of(new ElementoInteractivo(elementoId, id, tipoElemento, "Respuesta")),
                Map.of(), List.of(), null, Map.of(), List.of(solucion));
    }

    static VistaAcertijo vista() {
        return new VistaAcertijo(new RolesRepositoryEnMemoria(), new InventarioRepositoryEnMemoria());
    }

    @Override
    public Optional<DefinicionAcertijo> de(String matchId, String componenteMapaId) {
        return Optional.ofNullable(porComponente.get(componenteMapaId));
    }

    @Override
    public List<String> objetosDe(String matchId) {
        return List.of();
    }
}
