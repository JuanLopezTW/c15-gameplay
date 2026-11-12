package co.eci.c15.gameplay.domain;

import java.util.Objects;

public record ComponenteMapa(String id, TipoComponente tipo, Posicion posicion) {

    public ComponenteMapa {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(posicion, "posicion");
    }
}
