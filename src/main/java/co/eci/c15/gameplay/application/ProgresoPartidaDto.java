package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.ProgresoEquipo;

import java.util.List;

/** Foto del progreso de todos los equipos de una partida. */
public record ProgresoPartidaDto(String matchId, List<EquipoDto> equipos) {

    public record EquipoDto(String equipoId, int resueltos, int total, boolean completo, List<String> acertijosResueltos) {}

    public static ProgresoPartidaDto from(String matchId, List<ProgresoEquipo> progresos) {
        List<EquipoDto> equipos = progresos.stream()
                .map(p -> new EquipoDto(p.getEquipoId(), p.cantidadResueltos(), p.getTotal(), p.estaCompleto(), p.getResueltos()))
                .toList();
        return new ProgresoPartidaDto(matchId, equipos);
    }
}
