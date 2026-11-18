package co.eci.c15.gameplay.infrastructure.acertijos;

import co.eci.c15.gameplay.domain.CatalogoAcertijos;
import co.eci.c15.gameplay.domain.DefinicionAcertijo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Banco de acertijos en codigo. Con sorteo, cada partida toma 3 familias distintas y una variante de
 * cada una, siempre igual para el mismo matchId (los dos equipos ven los mismos acertijos y cualquier
 * replica llega al mismo resultado sin guardar nada). Sin sorteo usa siempre las 3 preguntas de siempre
 * (las pruebas de flujo lo necesitan para conocer las respuestas).
 */
@Component
public class BancoAcertijos implements CatalogoAcertijos {

    static final String PREFIJO = "acertijo-";
    static final int POR_PARTIDA = 3;

    private static final List<List<DefinicionAcertijo>> FAMILIAS = List.of(
            ContenidoAcertijos.PREGUNTAS, ContenidoAcertijos.GLOBOS, ContenidoAcertijos.ESTANTES,
            ContenidoAcertijos.CIFRADOS, ContenidoAcertijos.RELOJES, ContenidoAcertijos.CUADROS);

    private final boolean sorteo;

    public BancoAcertijos(@Value("${c15.acertijos.sorteo:true}") boolean sorteo) {
        this.sorteo = sorteo;
    }

    @Override
    public Optional<DefinicionAcertijo> de(String matchId, String componenteMapaId) {
        if (matchId == null || componenteMapaId == null || !componenteMapaId.startsWith(PREFIJO)) return Optional.empty();
        int indice;
        try {
            indice = Integer.parseInt(componenteMapaId.substring(PREFIJO.length())) - 1;
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        List<DefinicionAcertijo> deLaPartida = deLaPartida(matchId);
        return indice >= 0 && indice < deLaPartida.size() ? Optional.of(deLaPartida.get(indice)) : Optional.empty();
    }

    @Override
    public List<String> objetosDe(String matchId) {
        return deLaPartida(matchId).stream()
                .map(DefinicionAcertijo::objetoRequerido)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /** Los acertijos de la partida, en el orden de los puntos del mapa (acertijo-1, -2, -3). */
    List<DefinicionAcertijo> deLaPartida(String matchId) {
        if (!sorteo) return ContenidoAcertijos.PREGUNTAS;
        // Orden y variante salen de mezclar el matchId: no es azar de seguridad, solo variedad repetible
        List<List<DefinicionAcertijo>> familias = new ArrayList<>(FAMILIAS);
        familias.sort(Comparator.comparingInt(f -> mezcla(matchId + "|" + f.get(0).tipo())));
        List<DefinicionAcertijo> elegidos = new ArrayList<>(POR_PARTIDA);
        for (List<DefinicionAcertijo> familia : familias.subList(0, POR_PARTIDA)) {
            int variante = Math.floorMod(mezcla(matchId + "|variante|" + familia.get(0).tipo()), familia.size());
            elegidos.add(familia.get(variante));
        }
        return List.copyOf(elegidos);
    }

    /** Reparte bien los bits del hash (los hashCode de textos parecidos son parecidos). */
    static int mezcla(String texto) {
        int h = texto.hashCode();
        h ^= h >>> 16;
        h *= 0x7feb352d;
        h ^= h >>> 15;
        h *= 0x846ca68b;
        h ^= h >>> 16;
        return h;
    }
}
