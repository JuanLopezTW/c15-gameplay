package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.JugadorEnMapa;
import co.eci.c15.gameplay.domain.Posicion;

import java.util.List;

public record MovimientoDto(String userId, Posicion desde, Posicion hasta, List<Posicion> ruta) {

    public static MovimientoDto from(String userId, JugadorEnMapa.Movimiento movimiento) {
        return new MovimientoDto(userId, movimiento.desde(), movimiento.hasta(), movimiento.ruta());
    }
}
