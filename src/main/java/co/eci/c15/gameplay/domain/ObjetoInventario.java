package co.eci.c15.gameplay.domain;

import java.util.Objects;

/** Objeto que un equipo recolecto del mapa; {@code id} es el del componente del mapa. */
public record ObjetoInventario(String id, String nombre, String recolectadoPor) {

    public ObjetoInventario {
        Objects.requireNonNull(id);
        Objects.requireNonNull(nombre);
        Objects.requireNonNull(recolectadoPor);
    }
}
