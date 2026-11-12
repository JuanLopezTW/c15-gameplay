package co.eci.c15.gameplay.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "acertijos")
class AcertijoJpa {

    @Id
    private String id;
    @Column(nullable = false, unique = true)
    private String componenteMapaId;
    @Column(nullable = false, length = 2000)
    private String enunciado;

    protected AcertijoJpa() {}

    AcertijoJpa(String id, String componenteMapaId, String enunciado) {
        this.id = id;
        this.componenteMapaId = componenteMapaId;
        this.enunciado = enunciado;
    }

    String getId() { return id; }
    String getComponenteMapaId() { return componenteMapaId; }
    String getEnunciado() { return enunciado; }
}
