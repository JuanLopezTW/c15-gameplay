package co.eci.c15.gameplay.domain;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Decide si lo que envio un jugador coincide con la solucion de un acertijo. No distingue
 * mayusculas ni tildes ni espacios sobrantes. La respuesta puede venir en los mismos pasos
 * que la solucion (["rojo", "naranja"]) o partida en piezas que juntas la forman
 * (["E", "S", "C", "A", "P", "E"] para "escape"). El orden siempre importa.
 */
public final class ComparadorRespuestas {

    private ComparadorRespuestas() {}

    public static boolean coincide(List<String> respuesta, List<String> solucion) {
        if (respuesta == null || solucion == null || respuesta.isEmpty() || solucion.isEmpty()) return false;
        List<String> a = respuesta.stream().map(ComparadorRespuestas::normalizar).toList();
        List<String> b = solucion.stream().map(ComparadorRespuestas::normalizar).toList();
        return a.equals(b) || String.join("", a).equals(String.join("", b));
    }

    static String normalizar(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
