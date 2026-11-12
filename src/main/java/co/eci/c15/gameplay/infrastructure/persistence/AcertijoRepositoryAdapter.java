package co.eci.c15.gameplay.infrastructure.persistence;

import co.eci.c15.gameplay.domain.Acertijo;
import co.eci.c15.gameplay.domain.AcertijoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AcertijoRepositoryAdapter implements AcertijoRepository {

    private final AcertijoJpaRepository jpa;

    public AcertijoRepositoryAdapter(AcertijoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Acertijo> findByComponenteMapaId(String componenteMapaId) {
        return jpa.findByComponenteMapaId(componenteMapaId).map(this::toDomain);
    }

    @Override
    public Acertijo save(Acertijo a) {
        jpa.save(new AcertijoJpa(a.getId(), a.getComponenteMapaId(), a.getEnunciado()));
        return a;
    }

    private Acertijo toDomain(AcertijoJpa e) {
        return new Acertijo(e.getId(), e.getComponenteMapaId(), e.getEnunciado());
    }
}
