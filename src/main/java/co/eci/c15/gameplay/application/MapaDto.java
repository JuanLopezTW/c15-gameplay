package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.MapaIsometrico;
import co.eci.c15.gameplay.domain.Posicion;
import co.eci.c15.gameplay.domain.TipoComponente;

import java.util.List;

public record MapaDto(String matchId, int ancho, int alto, Posicion spawn, List<ComponenteDto> componentes) {

    public record ComponenteDto(String id, TipoComponente tipo, int x, int y) {}

    public static MapaDto from(MapaIsometrico mapa) {
        List<ComponenteDto> componentes = mapa.getComponentes().stream()
                .map(c -> new ComponenteDto(c.id(), c.tipo(), c.posicion().x(), c.posicion().y()))
                .toList();
        return new MapaDto(mapa.getMatchId(), mapa.getAncho(), mapa.getAlto(), mapa.getSpawn(), componentes);
    }
}
