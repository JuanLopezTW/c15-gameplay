package co.eci.c15.gameplay.domain;

import java.util.Objects;

public final class ElementoInteractivo {

    private final String id;
    private final String acertijoId;
    private final String tipo;
    private final String descripcion;

    public ElementoInteractivo(String id, String acertijoId, String tipo, String descripcion) {
        this.id = Objects.requireNonNull(id);
        this.acertijoId = Objects.requireNonNull(acertijoId);
        this.tipo = Objects.requireNonNull(tipo);
        this.descripcion = Objects.requireNonNull(descripcion);
    }

    public String getId() { return id; }
    public String getAcertijoId() { return acertijoId; }
    public String getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
}
