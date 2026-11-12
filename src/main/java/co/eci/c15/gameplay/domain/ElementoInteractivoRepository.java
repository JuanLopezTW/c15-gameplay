package co.eci.c15.gameplay.domain;

import java.util.List;

public interface ElementoInteractivoRepository {
    List<ElementoInteractivo> findByAcertijoId(String acertijoId);
    ElementoInteractivo save(ElementoInteractivo elemento);
}
