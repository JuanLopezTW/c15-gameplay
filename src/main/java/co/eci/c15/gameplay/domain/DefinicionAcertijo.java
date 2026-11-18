package co.eci.c15.gameplay.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Un acertijo del banco: lo que se muestra, como se dibuja ({@code tipo}) y sus respuestas aceptadas.
 * Las respuestas nunca salen del back.
 *
 * @param tipo            como lo dibuja el front: pregunta, globo, estante, cifrado, reloj o uv
 * @param datos           lo que ve todo el equipo (coordenadas, libros, la nota cifrada...)
 * @param datosPorJugador pistas repartidas: el grupo 0 es del Heroe y el 1 del Verdugo; vacio si no hay
 * @param objetoRequerido objeto del mapa que hace falta para ver {@code datosConObjeto} (null si ninguno)
 * @param soluciones      respuestas aceptadas; cada una en pasos (ver {@link ComparadorRespuestas})
 */
public record DefinicionAcertijo(
        String id,
        String tipo,
        String enunciado,
        List<ElementoInteractivo> elementos,
        Map<String, Object> datos,
        List<Map<String, Object>> datosPorJugador,
        String objetoRequerido,
        Map<String, Object> datosConObjeto,
        List<List<String>> soluciones
) {

    public DefinicionAcertijo {
        Objects.requireNonNull(id);
        Objects.requireNonNull(tipo);
        Objects.requireNonNull(enunciado);
        elementos = List.copyOf(elementos);
        datos = Map.copyOf(datos);
        datosPorJugador = List.copyOf(datosPorJugador);
        datosConObjeto = Map.copyOf(datosConObjeto);
        soluciones = List.copyOf(soluciones);
        if (soluciones.isEmpty()) throw new IllegalArgumentException("El acertijo " + id + " no tiene solucion");
        if (objetoRequerido == null && !datosConObjeto.isEmpty()) {
            throw new IllegalArgumentException("El acertijo " + id + " tiene datos ocultos sin objeto que los revele");
        }
    }

    public boolean acepta(List<String> respuesta) {
        return soluciones.stream().anyMatch(s -> ComparadorRespuestas.coincide(respuesta, s));
    }

    public boolean tienePistasRepartidas() {
        return !datosPorJugador.isEmpty();
    }

    /**
     * Lo que ve un jugador: los datos comunes, sus pistas y lo oculto si el equipo tiene el objeto.
     *
     * @param grupo       grupo de pistas del jugador (0 Heroe, 1 Verdugo)
     * @param todas       true si juega solo en su equipo: recibe todos los grupos para poder resolverlo
     * @param tieneObjeto si el equipo ya recogio el objeto requerido
     */
    public Map<String, Object> datosPara(int grupo, boolean todas, boolean tieneObjeto) {
        Map<String, Object> vista = new LinkedHashMap<>(datos);
        for (int i = 0; i < datosPorJugador.size(); i++) {
            if (todas || i == grupo % datosPorJugador.size()) unir(vista, datosPorJugador.get(i));
        }
        if (tieneObjeto) unir(vista, datosConObjeto);
        return vista;
    }

    /** Agrega los valores; si los dos son listas, las concatena (asi se juntan paginas o pistas de varios grupos). */
    private static void unir(Map<String, Object> destino, Map<String, Object> origen) {
        origen.forEach((clave, valor) -> destino.merge(clave, valor, (a, b) -> {
            if (a instanceof Collection<?> ca && b instanceof Collection<?> cb) {
                List<Object> juntas = new ArrayList<>(ca);
                juntas.addAll(cb);
                return juntas;
            }
            return b;
        }));
    }
}
