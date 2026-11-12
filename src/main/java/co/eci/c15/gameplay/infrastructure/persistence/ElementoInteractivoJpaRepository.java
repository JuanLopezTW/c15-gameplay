package co.eci.c15.gameplay.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ElementoInteractivoJpaRepository extends JpaRepository<ElementoInteractivoJpa, String> {
    List<ElementoInteractivoJpa> findByAcertijoId(String acertijoId);
}
