package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.InventarioEquipo;

import java.util.List;

/** Foto del inventario compartido de un equipo. */
public record InventarioDto(String matchId, String equipoId, List<ObjetoDto> objetos) {

    public record ObjetoDto(String id, String nombre, String recolectadoPor) {}

    public static InventarioDto from(InventarioEquipo inventario) {
        List<ObjetoDto> objetos = inventario.getObjetos().stream()
                .map(o -> new ObjetoDto(o.id(), o.nombre(), o.recolectadoPor()))
                .toList();
        return new InventarioDto(inventario.getMatchId(), inventario.getEquipoId(), objetos);
    }
}
