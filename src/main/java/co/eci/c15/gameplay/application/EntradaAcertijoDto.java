package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.AcertijoEnPartida;

import java.util.List;

/** Lo que el equipo lleva ingresado en un acertijo (clicks en orden). */
public record EntradaAcertijoDto(String componenteMapaId, List<String> entrada, boolean resuelto) {

    public static EntradaAcertijoDto from(AcertijoEnPartida estado) {
        return new EntradaAcertijoDto(estado.getComponenteMapaId(), estado.getEntrada(), estado.isResuelto());
    }
}
