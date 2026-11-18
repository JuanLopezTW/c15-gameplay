package co.eci.c15.gameplay.application;

import co.eci.c15.gameplay.domain.AcertijoEnPartida;
import co.eci.c15.gameplay.domain.DefinicionAcertijo;

import java.util.List;
import java.util.Map;

/**
 * Lo que ve un jugador de un acertijo. {@code tipo} dice como dibujarlo y {@code datos} trae lo que
 * necesita (sus pistas incluidas); nunca la solucion.
 */
public record VisualizarAcertijoDto(
        String id,
        String componenteMapaId,
        String tipo,
        String enunciado,
        String estado,
        boolean resuelto,
        boolean puedeEnviarSolucion,
        List<ElementoDto> elementos,
        Map<String, Object> datos,
        String objetoRequerido,
        boolean tieneObjeto
) {
    public record ElementoDto(String id, String tipo, String descripcion) {}

    public static VisualizarAcertijoDto from(DefinicionAcertijo acertijo, String componenteMapaId, AcertijoEnPartida estado,
                                             Map<String, Object> datos, boolean tieneObjeto) {
        List<ElementoDto> elementos = acertijo.elementos().stream()
                .map(e -> new ElementoDto(e.getId(), e.getTipo(), e.getDescripcion()))
                .toList();
        return new VisualizarAcertijoDto(
                acertijo.id(),
                componenteMapaId,
                acertijo.tipo(),
                acertijo.enunciado(),
                estado.getEstado().name(),
                estado.isResuelto(),
                !estado.isResuelto(),
                elementos,
                datos,
                acertijo.objetoRequerido(),
                tieneObjeto
        );
    }
}
