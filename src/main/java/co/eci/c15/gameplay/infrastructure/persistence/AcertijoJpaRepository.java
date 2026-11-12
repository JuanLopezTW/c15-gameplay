package co.eci.c15.gameplay.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface AcertijoJpaRepository extends JpaRepository<AcertijoJpa, String> {
    Optional<AcertijoJpa> findByComponenteMapaId(String componenteMapaId);
}
