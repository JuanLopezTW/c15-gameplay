package co.eci.c15.gameplay.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "elementos_interactivos")
class ElementoInteractivoJpa {

    @Id
    private String id;
    @Column(nullable = false)
    private String acertijoId;
    @Column(nullable = false)
    private String tipo;
    @Column(nullable = false)
    private String descripcion;

    protected ElementoInteractivoJpa() {}

    ElementoInteractivoJpa(String id, String acertijoId, String tipo, String descripcion) {
        this.id = id;
        this.acertijoId = acertijoId;
        this.tipo = tipo;
        this.descripcion = descripcion;
    }

    String getId() { return id; }
    String getAcertijoId() { return acertijoId; }
    String getTipo() { return tipo; }
    String getDescripcion() { return descripcion; }
}
