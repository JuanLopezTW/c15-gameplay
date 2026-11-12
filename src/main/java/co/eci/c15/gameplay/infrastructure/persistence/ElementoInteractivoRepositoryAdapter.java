package co.eci.c15.gameplay.infrastructure.persistence;

import co.eci.c15.gameplay.domain.ElementoInteractivo;
import co.eci.c15.gameplay.domain.ElementoInteractivoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ElementoInteractivoRepositoryAdapter implements ElementoInteractivoRepository {

    private final ElementoInteractivoJpaRepository jpa;

    public ElementoInteractivoRepositoryAdapter(ElementoInteractivoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<ElementoInteractivo> findByAcertijoId(String acertijoId) {
        return jpa.findByAcertijoId(acertijoId).stream()
                .map(e -> new ElementoInteractivo(e.getId(), e.getAcertijoId(), e.getTipo(), e.getDescripcion()))
                .toList();
    }

    @Override
    public ElementoInteractivo save(ElementoInteractivo e) {
        jpa.save(new ElementoInteractivoJpa(e.getId(), e.getAcertijoId(), e.getTipo(), e.getDescripcion()));
        return e;
    }
}
