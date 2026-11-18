package co.eci.c15.gameplay.infrastructure.acertijos;

import co.eci.c15.gameplay.domain.DefinicionAcertijo;
import co.eci.c15.gameplay.domain.ElementoInteractivo;

import java.util.List;
import java.util.Map;

/**
 * Contenido del banco de acertijos, agrupado por familia (un tipo de mecanica con varias variantes).
 * Cada partida toma 3 familias distintas y una variante de cada una (ver {@link BancoAcertijos}).
 */
final class ContenidoAcertijos {

    static final String LAMPARA_UV = "lampara-uv";

    private ContenidoAcertijos() {}

    /** Los 3 acertijos de siempre (preguntas): tambien son los fijos de las pruebas. */
    static final List<DefinicionAcertijo> PREGUNTAS = List.of(
            pregunta("secuencia", "Encuentra el número que falta en la secuencia: 2, 4, 8, ?, 32.",
                    "campo-numero", "Respuesta numérica", List.of("16")),
            pregunta("arcoiris", "Ordena los colores del arcoíris empezando por el rojo.",
                    "lista-ordenable", "Colores a ordenar",
                    List.of("rojo", "naranja", "amarillo", "verde", "azul", "añil", "violeta")),
            pregunta("cesar", "Descifra la palabra: cada letra se corrió una posición en el alfabeto (FTDBQF).",
                    "campo-texto", "Palabra descifrada", List.of("escape")));

    /** Globo terraqueo: coordenadas de un telegrama -> nombre de la ciudad. */
    static final List<DefinicionAcertijo> GLOBOS = List.of(
            globo("bogota", "STOP LLEGUÉ A LA CIUDAD DE LAS NUEVE LLUVIAS STOP", 4.71, -74.07, "4.71° N · 74.07° O",
                    List.of("Bogotá"), List.of("Bogota DC")),
            globo("paris", "STOP LA TORRE DE HIERRO ME VIGILA STOP", 48.86, 2.35, "48.86° N · 2.35° E",
                    List.of("París")),
            globo("tokio", "STOP AQUÍ SALE EL SOL PRIMERO STOP", 35.68, 139.69, "35.68° N · 139.69° E",
                    List.of("Tokio"), List.of("Tokyo")),
            globo("cairo", "STOP LAS PIRÁMIDES ESTÁN CERCA STOP", 30.04, 31.24, "30.04° N · 31.24° E",
                    List.of("El Cairo"), List.of("Cairo")),
            globo("lima", "STOP LA CIUDAD DE LOS REYES STOP", -12.05, -77.04, "12.05° S · 77.04° O",
                    List.of("Lima")));

    private static final List<Map<String, Object>> LIBROS = List.of(
            libro("Noche Polar", "#2e5aac", "azul", 1952),
            libro("El Faro", "#c0392b", "rojo", 1924),
            libro("Ciruelas", "#7d3c98", "violeta", 1938),
            libro("Arena Dorada", "#e1b12c", "amarillo", 1890),
            libro("Bosque Antiguo", "#2e8b57", "verde", 1871),
            libro("Atardecer", "#e67e22", "naranja", 1907));

    /** Libros en orden: arrastrar los libros del estante segun la nota de la bibliotecaria. */
    static final List<DefinicionAcertijo> ESTANTES = List.of(
            estante("color", "«Ordénalos como el arcoíris después de la tormenta»",
                    List.of("El Faro", "Atardecer", "Arena Dorada", "Bosque Antiguo", "Noche Polar", "Ciruelas")),
            estante("fecha", "«Del más antiguo al más reciente»",
                    List.of("Bosque Antiguo", "Arena Dorada", "Atardecer", "El Faro", "Ciruelas", "Noche Polar")),
            estante("alfabetico", "«Como en un diccionario»",
                    List.of("Arena Dorada", "Atardecer", "Bosque Antiguo", "Ciruelas", "El Faro", "Noche Polar")));

    private static final Map<String, Object> PAGINA_2 = pagina(2,
            "la niebla bajo el puente", "nadie cruzó aquella noche", "solo quedaba el farol");
    private static final Map<String, Object> PAGINA_3 = pagina(3,
            "el viejo guardián dormía", "sobre el banco de piedra", "con la llave en la mano");
    private static final Map<String, Object> PAGINA_5 = pagina(5,
            "las campanas sonaron tarde", "y el reloj de la torre", "marcó una hora imposible");
    private static final Map<String, Object> PAGINA_7 = pagina(7,
            "cuando la luna salió", "todos miraron al norte", "pero el tesoro seguía oculto");

    /** Cifrado de libro: cada jugador ve solo algunas paginas y se tienen que dictar las palabras. */
    static final List<DefinicionAcertijo> CIFRADOS = List.of(
            cifrado("reloj", "2-1-3 · 3-2-2 · 5-2-3", List.of("bajo el reloj")),
            cifrado("luna", "2-1-1 · 7-1-3 · 7-1-4", List.of("la luna salió")));

    /** Reloj de pendulo: la hora sale de pistas que tienen jugadores distintos. */
    static final List<DefinicionAcertijo> RELOJES = List.of(
            reloj("nueve",
                    List.of("I · Rayado en el marco de la puerta: 2 0 3",
                            "III · En el diario: «la campana sonó a los tres cuartos de hora»"),
                    List.of("II · Escrito bajo la alfombra: 1 1 2"),
                    "9:45", "09:45"),
            reloj("diez",
                    List.of("I · Tallado en la pata de la mesa: 4 1"),
                    List.of("II · En el espejo empañado: 3 0 2",
                            "III · En una carta: «nos vemos a la media hora en punto»"),
                    "10:30", "10:30"));

    /** Lampara UV: el cuadro solo muestra su mensaje si el equipo ya recogio la lampara del mapa. */
    static final List<DefinicionAcertijo> CUADROS = List.of(
            cuadro("condesa", "Retrato de la condesa",
                    "«LO QUE BRILLA CUANDO EL SOL DUERME: L _ N A»", List.of("luna")),
            cuadro("marinero", "El viejo marinero",
                    "«MI ENEMIGA, LEÍDA AL REVÉS: ATNEMROT»", List.of("tormenta")));

    private static DefinicionAcertijo pregunta(String id, String enunciado, String tipoElemento,
                                               String descripcion, List<String> solucion) {
        String acertijoId = "pregunta-" + id;
        return new DefinicionAcertijo(acertijoId, "pregunta", enunciado,
                List.of(elemento(acertijoId, tipoElemento, descripcion)), Map.of(), List.of(), null, Map.of(),
                List.of(solucion));
    }

    @SafeVarargs
    private static DefinicionAcertijo globo(String id, String telegrama, double latitud, double longitud,
                                            String coordenadas, List<String>... soluciones) {
        String acertijoId = "globo-" + id;
        return new DefinicionAcertijo(acertijoId, "globo",
                "Junto al globo terráqueo hay un telegrama con unas coordenadas. El nombre de esa ciudad abre la puerta.",
                List.of(elemento(acertijoId, "campo-texto", "Ciudad")),
                Map.of("telegrama", telegrama, "coordenadas", coordenadas, "latitud", latitud, "longitud", longitud),
                List.of(), null, Map.of(), List.of(soluciones));
    }

    private static Map<String, Object> libro(String titulo, String color, String nombreColor, int anio) {
        return Map.of("titulo", titulo, "color", color, "nombreColor", nombreColor, "anio", anio);
    }

    private static DefinicionAcertijo estante(String id, String nota, List<String> solucion) {
        String acertijoId = "estante-" + id;
        return new DefinicionAcertijo(acertijoId, "estante",
                "La bibliotecaria dejó una nota en el estante. Acomoda los libros (de izquierda a derecha) como dice.",
                List.of(elemento(acertijoId, "lista-ordenable", "Libros en orden")),
                Map.of("nota", nota, "libros", LIBROS), List.of(), null, Map.of(), List.of(solucion));
    }

    private static Map<String, Object> pagina(int numero, String... lineas) {
        return Map.of("numero", numero, "lineas", List.of(lineas));
    }

    private static DefinicionAcertijo cifrado(String id, String nota, List<String> solucion) {
        String acertijoId = "cifrado-" + id;
        return new DefinicionAcertijo(acertijoId, "cifrado",
                "Una nota dice «página-línea-palabra». Los libros quedaron repartidos: cada uno ve solo algunas"
                        + " páginas. Díctense las palabras por el chat de equipo.",
                List.of(elemento(acertijoId, "campo-texto", "Mensaje descifrado")),
                Map.of("nota", nota),
                List.of(Map.of("paginas", List.of(PAGINA_2, PAGINA_5)), Map.of("paginas", List.of(PAGINA_3, PAGINA_7))),
                null, Map.of(), List.of(solucion));
    }

    private static DefinicionAcertijo reloj(String id, List<String> pistasHeroe, List<String> pistasVerdugo,
                                            String hora, String horaConCero) {
        String acertijoId = "reloj-" + id;
        return new DefinicionAcertijo(acertijoId, "reloj",
                "El reloj de péndulo se detuvo. HORA = suma de los dígitos de las pistas I y II."
                        + " MINUTOS = lo que dice la pista III. Las pistas están repartidas entre ustedes.",
                List.of(elemento(acertijoId, "reloj", "Hora del péndulo")),
                Map.of(),
                List.of(Map.of("pistas", pistasHeroe), Map.of("pistas", pistasVerdugo)),
                null, Map.of(), List.of(List.of(hora), List.of(horaConCero)));
    }

    private static DefinicionAcertijo cuadro(String id, String titulo, String oculto, List<String> solucion) {
        String acertijoId = "uv-" + id;
        return new DefinicionAcertijo(acertijoId, "uv",
                "Un cuadro cuelga en la pared. Dicen que el pintor escribió algo con tinta invisible…",
                List.of(elemento(acertijoId, "campo-texto", "Palabra secreta")),
                Map.of("cuadro", titulo,
                        "sinObjeto", "Solo ves un cuadro polvoriento. Hace falta una lámpara UV: búsquenla en el mapa."),
                List.of(), LAMPARA_UV, Map.of("oculto", oculto), List.of(solucion));
    }

    private static ElementoInteractivo elemento(String acertijoId, String tipo, String descripcion) {
        return new ElementoInteractivo(acertijoId + "-elemento-1", acertijoId, tipo, descripcion);
    }
}
