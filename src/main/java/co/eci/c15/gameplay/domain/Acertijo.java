package co.eci.c15.gameplay.domain;

import java.util.Objects;

/**
 * Contenido de un acertijo del catálogo. Es el mismo para todas las partidas; el estado
 * (resuelto, bloqueado) de cada partida y equipo vive en {@link AcertijoEnPartida}.
 */
public final class Acertijo {

    private final String id;
    private final String componenteMapaId;
    private final String enunciado;

    public Acertijo(String id, String componenteMapaId, String enunciado) {
        this.id = Objects.requireNonNull(id);
        this.componenteMapaId = Objects.requireNonNull(componenteMapaId);
        this.enunciado = Objects.requireNonNull(enunciado);
    }

    public String getId() { return id; }
    public String getComponenteMapaId() { return componenteMapaId; }
    public String getEnunciado() { return enunciado; }
}
